# TrojanScheduler — Issues & Fixes Log

A running reference of bugs found, why they happened, and how they were fixed.

---

## Session 2026-03-08 (Backend bug fixes)

### Issue 1: Hardcoded DB password in application.properties
**File**: `backend/src/main/resources/application.properties` line 9
**Problem**: `spring.datasource.password=Gimnasia130_` was committed in plain text.
**Fix**: Changed to `${DB_PASSWORD:}` (empty default, must be provided via env var in prod).
**Reasoning**: Never commit secrets. The `${VAR:default}` Spring syntax lets the app still boot locally with an empty password if one isn't set, while requiring it in production.

---

### Issue 2: Wrong error shown for duplicate watchlist entry
**File**: `backend/src/main/java/com/trojanscheduler/auth/AuthExceptionHandler.java`
**Problem**: The `isDuplicateEmailConstraint` check used a broad `"Duplicate"` string match and ran before `isDuplicateWatchlistConstraint`. Adding a duplicate watchlist item triggered the email-duplicate error message instead of the correct watchlist error.
**Fix**: Moved the watchlist constraint check before the email constraint check.
**Reasoning**: More specific checks must always run before more general ones when pattern matching on exception messages.

---

### Issue 3: "Th" not parsed correctly in ConflictDetectionService
**File**: `backend/.../schedule/ConflictDetectionService.java`
**Problem**: The original draft included `Sa`/`Su` handling but not the correct "Th"→R normalization. USC uses "Th" for Thursday in its day codes.
**Fix**: `dayCode.toUpperCase().replace("TH", "R")` then `R → 4` (Thu). Removed unused Sa/Su cases.
**Reasoning**: USC API uses "TH" (uppercase in actual responses) where the two-char "Th" is the historical representation. By uppercasing first, we handle both forms.

---

### Issue 4: Infinite 401 retry loop in API client
**File**: `frontend/src/api/client.ts`
**Problem**: After a 401, the client called `refreshToken()` and retried, but the retry itself could also get a 401, triggering another refresh → infinite loop.
**Fix**: Added `_retried` flag on the Request object. If the flag is already set, skip the refresh attempt.
**Reasoning**: The retry-after-refresh pattern needs a guard to prevent recursion. Using a property on the request object is a clean way to thread state through fetch without changing the function signature.

---

### Issue 5: TBA warning not cleared on successful section add
**File**: `frontend/src/pages/Schedule.tsx`
**Problem**: `setTbaWarning('')` was only called in the conflict branch, not in the success branch when no TBA was involved. So if you first added a TBA section (warning shown), then added a non-TBA section successfully, the warning persisted.
**Fix**: Added `setTbaWarning('')` in the `else` (no TBA) success branch.
**Reasoning**: State should always be explicitly reset to its "clean" state when the condition that triggered it is no longer true.

---

### Issue 6: @Transactional no-op on self-call
**File**: `backend/.../polling/WatchlistPollingService.java` — `applySectionState()`
**Problem**: `@Transactional` was added to a private method called from within the same bean. Spring AOP creates a proxy, so self-calls bypass the proxy and the annotation has no effect.
**Fix**: Removed the `@Transactional` annotation and added a comment explaining why it can't be used there.
**Reasoning**: Spring's `@Transactional` only works when the method is called through the Spring proxy (i.e., from outside the bean). Self-calls skip the proxy entirely.

---

## Session 2026-03-09 (Frontend overhaul + API field mapping fixes)

### Issue 7: Wrong dayCode format in parseDayCode (frontend)
**File**: `frontend/src/utils/dayCode.ts`
**Problem**: The frontend `parseDayCode` used `code.replace(/Th/g, '4')` (case-sensitive, literal "Th"). But the USC API actually sends **uppercase** `"TH"` for Tuesday+Thursday. So "TH" would not be normalized and "H" would be silently skipped, meaning Thursday was dropped from the parsed result.
**Fix**: Changed to `code.toUpperCase().replace(/TH/g, 'R')` then `R → 3`, matching exactly what the backend `ConflictDetectionService` does.
**Reasoning**: Always verify field values against the live API, not assumptions in docs. The backend had the correct normalization; the frontend needed to mirror it.

---

### Issue 8: Wrong field names for section type and instructor
**File**: `frontend/src/types/course.ts`, `frontend/src/pages/Search.tsx`, `frontend/src/components/SwapSectionModal.tsx`
**Problem**: The `Section` type had `sectionType?: string` and `instructor?: string`, and the UI read those fields. The actual USC API response has:
- `rnrMode: "Lecture"` (not `sectionType`)
- `instructors: [{firstName, lastName}]` (not `instructor`)
**Fix**: Updated `Section` type to `rnrMode?: string` and `instructors?: SectionInstructor[]`. Added `formatInstructors()` helper. Updated all UI code to read the correct fields.
**Reasoning**: The USC API shape was documented from memory/assumptions, not verified against live responses. Always test against the actual API before writing display code.

---

### Issue 9: Wrong field name for course prefix
**File**: `frontend/src/pages/Search.tsx`, `frontend/src/pages/Schedule.tsx`
**Problem**: Code used `course.courseNumber` as the watchlist prefix and swap search query. The actual USC API Course object has no `courseNumber` field; the subject prefix (e.g., "CSCI") is in `course.prefix`.
**Fix**: Changed all references to `course.prefix ?? course.courseNumber`. Updated `ScheduledSection.courseNumber` to store `course.prefix` when adding to schedule.
**Reasoning**: Same root cause as Issue 8 — field names were assumed, not verified. The `fullCourseName` ("CSCI 380") is for display; `prefix` ("CSCI") is for search queries.

---

### Issue 11: LazyInitializationException on token refresh (500 error)
**File**: `backend/.../auth/AuthService.java` — `refresh()`
**Problem**: `refresh()` called `refreshTokenService.rotateAndSetCookie()`, which is `@Transactional`. When that method returned, the Hibernate session closed. `refresh()` then called `user.getEmail()` on the returned `User` object — but since `RefreshToken.user` is a lazy-loaded `@ManyToOne`, the `User` was a Hibernate proxy that could no longer be initialized (no open session). This caused a `LazyInitializationException` and a 500 response.
**Fix**: Added `@Transactional` to `AuthService.refresh()`. The outer transaction keeps the Hibernate session alive for the full method, so the proxy can be resolved when `user.getEmail()` is called.
**Reasoning**: When a `@Transactional` method calls another `@Transactional` method, they join the same transaction — the session stays open until the outermost transaction commits. Without `@Transactional` on `refresh()`, each call to `rotateAndSetCookie()` opened and closed its own transaction, leaving a detached proxy behind.

---

### Issue 10: sockjs-client missing TypeScript types
**File**: `frontend/src/hooks/useNotifications.ts`
**Problem**: `npm run build` failed with `TS7016: Could not find a declaration file for module 'sockjs-client'`.
**Fix**: Ran `npm i --save-dev @types/sockjs-client`.
**Reasoning**: `sockjs-client` ships without bundled TypeScript types. The `@types/` package on DefinitelyTyped provides them. This was a pre-existing issue that only surfaced when strict TypeScript checking was enforced.

---
