# Trojan Scheduler — Design Choices and Rationale

This document explains the main design decisions in the project and why they were made. It lives in the same directory as `RUN_AND_TEST.md` and the main app code.

---

## 1. Overall architecture

- **Backend:** Spring Boot (Java 21), REST API, MySQL, no server-side sessions.
- **Frontend:** React (TypeScript) with Vite, single-page app, talks to backend only via HTTP and WebSocket.
- **No server-side schedule storage:** The “schedule” (list of sections the user is considering) is kept in the browser (React state + `localStorage`). Only conflict checking is done on the server.

**Why:** Keeps the backend focused on auth, course data, watchlist, and notifications. Schedule builder is a client-side workflow with a single server call (conflict check). Persisting schedules to the DB could be added later without changing these choices.

---

## 2. Authentication: JWT + refresh cookie

- **Access token:** Short-lived JWT in the response body (and often stored in memory or state on the client). Used in the `Authorization: Bearer …` header for API and WebSocket.
- **Refresh token:** Long-lived, stored in an HTTP-only cookie, used only on `/api/auth/refresh` to get a new access token.
- **Session:** Stateless; no server-side session store.

**Why:** JWTs allow stateless API auth and easy use in WebSocket CONNECT headers. Short-lived access tokens limit exposure if leaked. The refresh token in an HTTP-only cookie is not readable by JavaScript, so it’s a safer way to renew access. Stateless design avoids session storage and scales simply.

---

## 3. Security configuration

- **CSRF disabled:** The API is used by a separate frontend (different origin) with Bearer tokens; classic cookie-based CSRF doesn’t apply. Refresh is done with same-site cookie and optional CSRF could be added for the cookie endpoint if needed.
- **CORS:** Explicit allowlist for `http://localhost:5173` and `http://localhost:3000` with credentials allowed so the refresh cookie is sent.
- **Public routes:** `/api/auth/**`, `/actuator/health`, `/api/courses/search`, `/ws`, `/ws/**` are permitted without auth so login, health checks, course search, and WebSocket handshake work. Everything under `/api/**` except the above requires authentication.

**Why:** Course search is public so users can see results before logging in. WebSocket endpoint must be reachable for the handshake; actual subscription is tied to the user after JWT validation on CONNECT.

---

## 4. USC course data: proxy, cache, and resilience

- **Backend proxies all USC requests:** The frontend never calls `classes.usc.edu` directly. The backend calls the USC API (same path style: e.g. `/api/Search/Basic?termCode=…&searchTerm=…`) and returns the JSON.

**Why:** Hides USC’s URL and shape from the client, allows caching and error handling in one place, and avoids CORS and credential issues with a third-party API.

- **Search cache (DB):** Successful USC search responses are stored in `search_cache` keyed by `(termCode, searchKey)` with a TTL (e.g. 5 minutes). Repeated identical searches are served from cache.

**Why:** Reduces load on the USC API and speeds up repeated searches; TTL keeps data reasonably fresh.

- **Term and query normalization:** The backend trims and takes the first token of the “term” so values like `29200 D` or `20263` are sent as a valid numeric term. If the user types a course name (e.g. `MATH`) in the term field and leaves search empty, the backend treats it as swapped and uses a default term with the course as the search query.

**Why:** USC expects a numeric term code; this avoids 400s from minor input mistakes and makes the UI more forgiving.

- **USC timeout and fallback:** If the USC request times out or fails (e.g. 4xx/5xx), the backend does not return 5xx. It returns 200 with a JSON body like `{"courses":[],"error":"USC API timed out. Please try again in a moment."}`.

**Why:** Search “works” even when USC is slow or down: the UI shows a clear message and an empty list instead of a generic error or stack trace. The frontend can always parse the response and show the `error` field when present.

- **Read timeout and retries:** The USC client uses a configurable read timeout (e.g. 25s) and retries with backoff on 5xx. Timeouts are mapped to a single, clear error message and 503-style handling.

**Why:** External APIs can be slow or flaky; longer timeout and retries improve success rate; consistent error handling keeps the UX predictable.

---

## 5. Watchlist and polling (“Option B”)

- **Watchlist:** Per-user list of (termCode, sisSectionId, optional coursePrefix). Used to know which sections to poll and whom to notify when a seat opens.
- **Course prefix for polling:** When present, `coursePrefix` (e.g. `MATH`, `CSCI`) is used to group watchlist entries. Polling is done by distinct `(termCode, coursePrefix)`: one USC search per group returns all sections for that course; then snapshots and notifications are updated for each watched section in that group.

**Why:** One search per course prefix instead of one per section reduces the number of calls to the USC API and fits how their search API works (search by term + prefix returns many sections).

- **Section snapshots:** Last known seat state (open/closed, capacity, enrolled, openSeats) is stored in `section_snapshot` so we can detect the transition “was full, now has seats” and emit a single “seat opened” event per user per section per time window.
- **Event fingerprint and dedupe:** “Seat opened” events are deduped by a fingerprint (event type + term + section + time bucket) so the same opening doesn’t create duplicate notifications if polling runs multiple times in a short window.

**Why:** We only notify when we observe a change (0 → &gt;0 open seats), and we avoid spamming the user for the same event.

---

## 6. Notifications and WebSocket

- **Channel:** STOMP over WebSocket (SockJS fallback), endpoint `/ws`, user-specific queue `/user/queue/notifications`.
- **Auth:** On CONNECT, the client sends `Authorization: Bearer <accessToken>`. The server validates the JWT and attaches a principal whose name is the user id; messages to that user are sent to that principal’s queue.

**Why:** STOMP gives a simple pub/sub model; user destination ensures each user only gets their own notifications. JWT on CONNECT keeps the same auth model as the REST API.

- **When we send:** A notification is sent when the polling service creates a new “seat opened” event (after persisting the notification event and checking dedupe). No notification is sent for other event types unless we add them later.

**Why:** Real-time feedback for the main user-facing event (a seat opened on a watched section) without polling the API from the client.

---

## 7. Conflict detection

- **API:** POST `/api/schedule/check-conflict` with `existingMeetings` and `candidateMeetings` (each a list of `{ dayCode, startTime, endTime }`). Response indicates whether there is a conflict, whether TBA was involved, and an optional message.
- **Logic:** Day codes (e.g. `MW`, `TTh`, `TR`, `F`) are normalized to a set of weekdays; times are parsed to minutes-from-midnight. Two meetings conflict if they share at least one day and their time ranges overlap (start1 &lt; end2 and start2 &lt; end1). If any meeting has null/blank start or end, the response sets `tbaInvolved` so the UI can warn that the result may be uncertain.

**Why:** Matches USC’s common day/time shape and supports TBA sections without guessing. The API is stateless and doesn’t need to store the user’s schedule; the client sends the relevant slice of schedule each time.

- **Schedule on the client:** The built schedule is kept in React state and `localStorage` (key `trojanscheduler-schedule`). Conflict check is called when the user tries to add a section (e.g. after “Add to schedule” from search), with existing schedule meetings and the new section’s meetings.

**Why:** Schedule is a builder workspace; persisting it in the DB was deferred. Local storage gives persistence across reloads and a single source of truth for the conflict-check request.

---

## 8. Database and schema

- **MySQL:** Chosen as the main database for users, refresh tokens, watchlist, search cache, section snapshots, and notification events. Schema is managed by Flyway (V1–V5) with `spring.jpa.hibernate.ddl-auto=validate` so the app doesn’t change the schema at runtime.
- **H2 in tests:** Test profile uses an in-memory H2 database and no Flyway so tests run without a real MySQL instance.

**Why:** MySQL is common and suitable for this data size and access pattern. Flyway keeps migrations explicit and repeatable; validate ensures production doesn’t drift from migrations.

- **watchlist.priority:** Stored as INT in the DB (migration V5) to match JPA’s integer type and avoid type mismatch with a previous TINYINT definition.

---

## 9. Frontend stack and UX

- **Vite + React + TypeScript:** Fast dev server and builds, type safety, and a simple component model. No global state library; context is used for auth and schedule.
- **Single HTML entry, “Loading” and error handling:** `index.html` includes a small inline script that sets `window.global` for sockjs-client (browser doesn’t define `global`), and a visible “Loading Trojan Scheduler…” message. If the app script fails to run, a follow-up script can replace that with an “App didn’t start” message and a hint to check the console.

**Why:** Avoids blank or white screen when something fails early (e.g. missing `global`). Users always see either the app or a clear next step.

- **Search labels and layout:** Term code and “Course or subject” are clearly labeled (with hints like “digits, e.g. 20263” and “e.g. MATH, CSCI”) so users are less likely to swap the two fields. API error messages (e.g. timeout) are shown above the form when the response includes an `error` field.

**Why:** Reduces 400s and confusion; when USC is unavailable, the user sees a direct message instead of a generic “Search failed.”

---

## 10. Root endpoint and errors

- **GET `/`:** Returns a short plain-text message pointing to the frontend and API docs instead of a Whitelabel error page.

**Why:** If someone opens the backend URL in a browser, they get a clear explanation instead of “No static resource.”

- **Exception handling:** `AuthExceptionHandler` (and related) map known exceptions (e.g. `UscException`, validation, duplicate email/watchlist) to appropriate HTTP status codes and JSON error bodies. Timeouts and other USC failures are turned into a single `UscException` with a clear message and, where appropriate, 503.

**Why:** Consistent, safe error responses and a better experience when external services fail.

---

## Summary table

| Area              | Choice                          | Reason |
|-------------------|----------------------------------|--------|
| Auth              | JWT + refresh cookie, stateless  | Secure, scalable, works with WebSocket |
| Course search     | Backend proxy + DB cache         | One place to handle USC, cache, and errors |
| Search on failure | 200 + `{"courses":[],"error":"…"}` | Search UI always works; message explains failure |
| Watchlist polling | By (termCode, coursePrefix)     | Fewer USC calls, matches search API |
| Notifications     | STOMP, user queue, JWT on CONNECT | Real-time, per-user, same auth as API |
| Schedule          | Client state + localStorage     | Simple; conflict check is the only server call |
| Conflict check    | Day + time overlap, TBA flag    | Matches USC data; honest about uncertainty |
| DB                | MySQL + Flyway, H2 in tests     | Clear schema history; tests without MySQL |

This file is the single place that documents these design choices and their rationale in the same directory as the runbook (`RUN_AND_TEST.md`).
