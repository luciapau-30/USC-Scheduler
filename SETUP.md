# TrojanScheduler — Setup & User Guide

A full-stack USC course scheduler. Search courses, build a visual weekly calendar, watch full sections for seat openings, and get real-time notifications.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Project structure](#2-project-structure)
3. [Backend setup](#3-backend-setup)
4. [Frontend setup](#4-frontend-setup)
5. [Running the app](#5-running-the-app)
6. [Using the app — full walkthrough](#6-using-the-app--full-walkthrough)
   - [Register / Login](#register--login)
   - [Search courses](#search-courses)
   - [Add a section to your schedule](#add-a-section-to-your-schedule)
   - [The weekly calendar](#the-weekly-calendar)
   - [Edit / swap a section](#edit--swap-a-section)
   - [The watchlist](#the-watchlist)
   - [Seat notifications](#seat-notifications)
7. [What syncs across devices, what doesn't](#7-what-syncs-across-devices-what-doesnt)
8. [Environment variables reference](#8-environment-variables-reference)
9. [Troubleshooting](#9-troubleshooting)

---

## 1. Prerequisites

| Tool | Required version | Check |
|------|-----------------|-------|
| Java (JDK) | 21 | `java -version` |
| Maven | 3.8+ (or use `./mvnw`) | `mvn -v` |
| MySQL | 8.x | `mysql --version` |
| Node.js | 18+ | `node --version` |
| npm | 9+ | `npm --version` |

> You do **not** need Maven installed globally — `./mvnw` (Maven Wrapper) is included in the `backend/` folder and works out of the box.

---

## 2. Project structure

```
TrojanScheduler/
├── backend/              Spring Boot 3.5 API (port 8080)
│   ├── src/main/java/com/trojanscheduler/
│   │   ├── auth/         Login, register, JWT, refresh tokens
│   │   ├── courses/      USC course search proxy, terms endpoint
│   │   ├── polling/      Background seat-check polling
│   │   ├── schedule/     Conflict detection
│   │   ├── usc/          HTTP client for classes.usc.edu
│   │   └── watchlist/    Watchlist CRUD
│   └── src/main/resources/
│       ├── application.properties
│       └── db/migration/ Flyway SQL migrations (V1–V5)
│
├── frontend/             React 18 + TypeScript (port 5173)
│   └── src/
│       ├── api/          HTTP client, auth, courses, schedule, watchlist
│       ├── components/   Calendar, Sidebar, Modal, Toast, Badges
│       ├── context/      Auth state, Schedule state (localStorage)
│       ├── hooks/        WebSocket notifications
│       ├── pages/        Search, Schedule, Watchlist, Login, Register
│       ├── types/        USC API types
│       └── utils/        dayCode parser
│
├── CLAUDE.md             Architecture reference for developers
├── ISSUES.md             Bug log with reasoning
└── SETUP.md              This file
```

---

## 3. Backend setup

### 3a. Create the MySQL database

MySQL creates the database automatically on first run (`createDatabaseIfNotExist=true` in the JDBC URL), but your MySQL user needs permission to do so. The simplest approach:

```bash
mysql -u root -p
```

```sql
-- Optional: create a dedicated user instead of using root
CREATE USER 'trojan'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON trojanscheduler.* TO 'trojan'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

If you keep using root, just make sure you know your root password.

### 3b. Configure environment variables

The backend reads secrets from environment variables. Create a small shell script or set them in your terminal:

```bash
export DB_PASSWORD=your_mysql_password   # required
export DB_USERNAME=root                  # default is root
export DB_URL=jdbc:mysql://localhost:3306/trojanscheduler?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
export JWT_SECRET=change-me-to-a-long-random-string-at-least-32-chars
```

> In development, only `DB_PASSWORD` is strictly required — all other values have sensible defaults in `application.properties`.

### 3c. Database migrations

Flyway runs automatically on startup. It applies the SQL files in `backend/src/main/resources/db/migration/` in order:

| Migration | What it creates |
|-----------|----------------|
| V1 | `users`, `refresh_tokens` |
| V2 | `watchlist` |
| V3 | `search_cache`, `section_snapshot` |
| V4 | Adds `course_prefix` column to watchlist |
| V5 | Changes watchlist priority to integer |

You never run migrations manually — just start the backend and Flyway handles it.

---

## 4. Frontend setup

```bash
cd frontend
npm install
```

The frontend needs to know where the backend is. By default it assumes `http://localhost:8080`. If your backend is on a different host/port, create a `.env.local` file:

```bash
# frontend/.env.local
VITE_API_URL=http://localhost:8080
```

---

## 5. Running the app

Open **two terminals**.

**Terminal 1 — Backend:**
```bash
cd backend
DB_PASSWORD=your_password ./mvnw spring-boot:run
```

You should see:
```
Started BackendApplication in X.XXX seconds
```

Health check: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`

**Terminal 2 — Frontend:**
```bash
cd frontend
npm run dev
```

You should see:
```
  VITE v5.x  ready in XXX ms
  ➜  Local:   http://localhost:5173/
```

Open **http://localhost:5173** in your browser.

---

## 6. Using the app — full walkthrough

### Register / Login

The first page you see is the login screen. Click **"Register"** to create a new account.

- **Email**: any valid email format
- **Password**: minimum 6 characters

After registering you are automatically logged in and redirected to the Search page. Your account is saved to the database — you can log in from any browser or device using the same credentials.

**Sessions**: You stay logged in for 15 minutes of inactivity, then the app silently refreshes your session using a secure cookie (valid for 14 days). You only have to log in again after 14 days of no use, or if you explicitly log out.

---

### Search courses

Navigate to **Search** in the top nav.

**Step 1 — Pick a term:**
Select a term from the dropdown (Fall 2026, Spring 2026, etc.). If your term isn't listed, choose "Other…" and type the 5-digit USC term code (e.g. `20263`).

| Term | Code |
|------|------|
| Fall 2026 | 20263 |
| Spring 2026 | 20261 |
| Summer 2026 | 20256 |
| Fall 2025 | 20253 |
| Spring 2025 | 20251 |

**Step 2 — Enter a subject or course:**
Type a subject prefix (e.g. `CSCI`, `MATH`, `BISC`) or a full course prefix (e.g. `CSCI 101`). Hit **Search**.

**Reading the results:**

Each result card is one course. Inside it, each row is one section (a specific time slot for that course):

```
§ 29934   [Lecture]   TH 9:30am–10:50am   Prof. Smith   [Open 12/60]
                                                         [+ Watchlist]  [+ Schedule]
```

- **§ number** — the SIS section ID (USC's internal section identifier)
- **Badge** (Lecture, Lab, etc.) — the meeting type
- **Schedule** — days and times. "TH" = Tuesday + Thursday. "TBA" = time not yet set.
- **Instructor** — the assigned professor (if listed)
- **Seat badge**:
  - Green **Open X/Y** — seats available
  - Red **Full X/Y** — no open seats (add to watchlist!)
  - Gray **Cancelled** — section was cancelled

**Conflict preview:** If a section overlaps with something already on your schedule, it will appear dimmed with a ⚠ warning. This is a client-side preview only — the actual conflict check happens when you click "+ Schedule".

**Hide cancelled toggle:** On by default. Hides cancelled sections so they don't clutter your results. Uncheck to show them.

---

### Add a section to your schedule

Click **"+ Schedule"** on any section row.

The app sends the section's meeting times to the backend conflict checker, which compares against every meeting already on your schedule:

- **No conflict** → section is added to your calendar immediately
- **Conflict** → red error message appears at the top of the Schedule page explaining which times overlap. Nothing is added.
- **TBA time involved** → yellow warning: the conflict check may be incomplete because the time is unknown. The section is still added.

---

### The weekly calendar

The **Schedule** page has two panels:

**Left — Weekly Calendar (Mon–Fri, 7am–10pm)**

Each section you've added appears as a colored block on the days it meets. Colors cycle through a palette of 8 — each section gets its own color when added.

- Sections that meet multiple days (e.g. MWF) show a block on each day
- Sections that meet at overlapping times on the same day are shown side by side
- TBA sections (no time set) do NOT appear on the calendar — they are in the sidebar only

On each block you'll see two buttons:
- **Edit** → opens the Swap Section modal
- **✕** → removes the section from your schedule

**Right — Sidebar**

A list of all your added sections including TBA ones. Shows:
- Colored dot (matches the calendar block color)
- Course title
- Instructor (if available)
- Meeting time(s), or "TBA" badge
- **Edit** and **Remove** buttons

---

### Edit / swap a section

Click **Edit** on any calendar block or sidebar row. A modal opens showing all sections of the same course for the same term.

Each row shows the section's time, type, instructor, and seat count. Your current section is highlighted as "Current".

Click **Swap** on any other row to switch to it:
- The app runs a conflict check first (excluding your current section from existing meetings, since it's being replaced)
- If no conflict → your calendar updates immediately to the new section, keeping the same color
- If conflict → the conflict message appears inline in the modal

---

### The watchlist

When a section is **Full** and you still want to get into it, add it to your watchlist.

Click **"+ Watchlist"** on any section in Search. You are redirected to the Watchlist page.

**The watchlist page shows:**
- Every section you're watching
- The term label (e.g. "Fall 2026")
- A **"View in search →"** link that takes you back to Search pre-filled with that course

**To remove a watched section:** click **Remove** on its row.

**How polling works:** The backend checks seat availability every ~10 minutes (with some random jitter to avoid hitting USC's API in synchronized bursts). It groups your watchlist by subject (e.g. all CSCI sections together) and does one USC API call per group, not one per section. This keeps it efficient.

---

### Seat notifications

When the backend detects that a watched section opened up (was full → now has seats), it sends a real-time notification to your browser via WebSocket.

A green toast banner appears in the **top-right corner** of any page:

```
● Seat opened for section 29934 (term 20263)
  [View watchlist]  ✕
```

- Click **View watchlist** to jump directly to your watchlist
- Click **✕** to dismiss it manually
- It auto-dismisses after 10 seconds

Notifications only arrive while you have the app open in a browser tab. They are not push notifications — if the browser is closed, you won't see it (but the seat may still be available when you check later).

---

## 7. What syncs across devices, what doesn't

| Feature | Synced (server) | Not synced (local) |
|---------|----------------|-------------------|
| Login credentials | ✅ Log in from any device | |
| Watchlist | ✅ Add on laptop, see on phone | |
| Seat notifications | ✅ Polling runs server-side | |
| **Built schedule / calendar** | | ❌ Browser `localStorage` only |

**The schedule is stored in your browser's localStorage**, not in the database. This means:

- Your schedule on your laptop is invisible to your phone and vice versa
- Clearing browser data or using incognito mode wipes your schedule
- Different browsers on the same computer have separate schedules

**Practical tip:** If you want your schedule visible on multiple devices, take a screenshot or note down the section IDs and re-add them on each device. Each device only takes a few seconds to rebuild since search is fast.

---

## 8. Environment variables reference

All have defaults for local development. Only `DB_PASSWORD` is required if MySQL uses a password.

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:mysql://localhost:3306/trojanscheduler?...` | JDBC connection string |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | *(empty)* | MySQL password — **set this** |
| `JWT_SECRET` | `dev-change-me-...` | HMAC signing key for access tokens (use 32+ random chars in prod) |
| `JWT_ISSUER` | `trojan-scheduler` | JWT `iss` claim |
| `JWT_ACCESS_TTL_SECONDS` | `900` (15 min) | How long an access token is valid |
| `REFRESH_TTL_SECONDS` | `1209600` (14 days) | How long a refresh cookie is valid |
| `USC_BASE_URL` | `https://classes.usc.edu` | USC course data API base |
| `USC_CONNECT_TIMEOUT_MS` | `15000` | Connect timeout to USC API |
| `USC_READ_TIMEOUT_MS` | `25000` | Read timeout to USC API |
| `USC_RETRY_MAX_ATTEMPTS` | `3` | Retries on USC 5xx errors |
| `CACHE_SEARCH_TTL_SECONDS` | `300` (5 min) | How long course search results are cached |
| `CACHE_PROGRAMS_TTL_SECONDS` | `3600` (1 hour) | How long the USC programs list is cached |
| `POLLING_INTERVAL_MS` | `600000` (10 min) | Watchlist polling frequency |
| `POLLING_JITTER_MS` | `60000` (1 min) | Random jitter added to polling interval |
| `POLLING_INITIAL_DELAY_MS` | `30000` (30 sec) | Delay before first poll after startup |

---

## 9. Troubleshooting

**Backend won't start — "Access denied for user 'root'"**
Your `DB_PASSWORD` env var is wrong or not set. Double-check with:
```bash
mysql -u root -pyour_password -e "SELECT 1"
```

**Backend won't start — "Table 'X' doesn't exist"**
Flyway migration failed. Check the logs for the SQL error. Usually means the DB user doesn't have enough permissions. Grant `ALL PRIVILEGES` on the database.

**Backend won't start — "Caused by: org.flywaydb.core.api.exception.FlywayValidateException"**
The migration history in the DB doesn't match the migration files on disk (e.g. a file was edited after running). In dev, you can delete the `flyway_schema_history` table and all app tables, then restart to re-run from scratch.

**Frontend shows "Could not reach backend"**
The backend isn't running or is on a different port. Make sure `./mvnw spring-boot:run` is running in another terminal and `http://localhost:8080/actuator/health` returns `{"status":"UP"}`.

**Search returns "Course service: ..." error**
The USC API (`classes.usc.edu`) is unreachable or rate-limited. Wait a moment and try again. The backend retries up to 3 times with backoff automatically.

**Search returns empty results**
- Make sure the subject code exists (e.g. `CSCI` not `CS`)
- Make sure the term code is correct and active at USC (e.g. `20263` for Fall 2026)
- Some subjects only exist in certain schools — the backend resolves the school automatically

**Notifications not appearing**
WebSocket connects on login. If you were logged in before the backend restarted, the WS connection may be broken. Log out and back in to reconnect.

**Schedule disappeared**
The schedule lives in browser `localStorage`. It's gone if you:
- Cleared browser site data
- Opened a different browser
- Used incognito / private mode
- Used a different device

Re-add your sections from Search.

**"Section already in watchlist" error when adding**
You already have this exact section (same term + section ID) in your watchlist. It shows up on the Watchlist page.
