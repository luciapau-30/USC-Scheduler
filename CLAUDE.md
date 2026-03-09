# TrojanScheduler — Developer Guide

## Overview
Full-stack USC course scheduler. Students search courses, build a visual weekly schedule, watchlist sections, and receive real-time seat notifications via WebSocket.

## Architecture
- **Backend**: Spring Boot 3.5, Java 21, MySQL 8 + Flyway, JWT auth (access token in memory, refresh token in HTTP-only cookie)
- **Frontend**: React 18, TypeScript, Vite, STOMP/SockJS WebSocket
- **Auth flow**: POST `/api/auth/login` → `TokenResponse` (access token) + `Set-Cookie` (refresh token)
- **Schedule storage**: Browser `localStorage` only — no server-side schedule

## Key Invariants
- `CourseSearchService.search()` never throws — returns `{courses:[], error:"..."}` on USC API failure
- Conflict check: `POST /api/schedule/check-conflict` with `{existingMeetings, candidateMeetings}`
- USC day codes: `M T W Th F` — "Th" is normalized to index 3 (Thursday) in both frontend and backend
- Watchlist polling: one USC search per `(termCode, coursePrefix)` group, not per section
- Notification dedup: SHA-256 fingerprint bucketed by 5-minute windows

## USC API Shape
```json
{
  "courses": [{
    "title": "Introduction to Computing",
    "courseNumber": "CSCI 101",
    "sections": [{
      "sisSectionId": "12345",
      "sectionType": "Lecture",
      "instructor": "John Smith",
      "location": "SAL 101",
      "totalSeats": 30,
      "registeredSeats": 28,
      "isFull": false,
      "isCancelled": false,
      "schedule": [{"dayCode": "MWF", "startTime": "10:00", "endTime": "11:00"}]
    }]
  }]
}
```

## Environment Variables (required in prod)
| Variable | Description |
|----------|-------------|
| `DB_PASSWORD` | MySQL password |
| `DB_URL` | JDBC URL |
| `DB_USERNAME` | MySQL username |
| `JWT_SECRET` | HS256 key (32+ bytes) |
| `JWT_ISSUER` | Token issuer |
| `JWT_ACCESS_TTL_SECONDS` | Access token lifetime |
| `REFRESH_TTL_SECONDS` | Refresh token lifetime |

## File Map
```
backend/src/main/java/com/trojanscheduler/
  auth/          — JWT, SecurityConfig, login/register/refresh/logout
  courses/       — USC proxy (search), TermsController
  polling/       — WatchlistPollingService, @Scheduled polling job
  schedule/      — ConflictDetectionService (day-code parsing)
  usc/           — UscClient (HTTP to USC API)
  watchlist/     — WatchlistService, entity, repository

backend/src/main/resources/
  db/migration/  — Flyway V1–V5 (users, refresh_tokens, watchlist, search_cache, notification_log)
  application.properties

frontend/src/
  api/           — client.ts (JWT retry), auth.ts, courses.ts, schedule.ts, watchlist.ts
  components/    — Layout, ErrorBoundary, WeeklyCalendar, ScheduleSidebar, SeatBadge, NotificationToast, SwapSectionModal
  context/       — AuthContext, ScheduleContext (localStorage)
  hooks/         — useNotifications (STOMP/SockJS)
  pages/         — Search, Schedule, Watchlist, Login, Register
  types/         — course.ts (Section, Course, SearchResponse)
  utils/         — dayCode.ts
```

## Common Mistakes to Avoid
1. **Day codes**: USC uses "Th" for Thursday, NOT "R". Frontend `parseDayCode` and backend `ConflictDetectionService` both normalize "Th"→3.
2. **Auth check order in AuthExceptionHandler**: watchlist constraint check MUST come before email constraint check (broader "Duplicate" match would swallow watchlist errors).
3. **`@Transactional` in same class**: Spring AOP proxy is bypassed for self-calls — don't add `@Transactional` to private methods called from within the same bean.
4. **`_retried` flag in client.ts**: The 401 retry logic uses `_retried` on the request to prevent infinite refresh loops.
5. **Schedule localStorage key**: `'trojanscheduler-schedule'` — changing this loses users' saved schedules.
6. **`CourseSearchService`**: Returns soft error JSON — callers should check `data.error`, not expect a thrown exception.

## Running Locally
```bash
# Backend (requires MySQL running, env vars set)
cd backend && mvn spring-boot:run

# Frontend
cd frontend && npm run dev   # http://localhost:5173

# Build check
cd frontend && npm run build
```

## Calendar Color Palette (cycles by section add order)
`#58a6ff #3fb950 #d29922 #f0883e #bc8cff #ff7b72 #39d353 #79c0ff`

## USC Terms (hardcoded in TermsController + Search page)
| Term Code | Label |
|-----------|-------|
| 20263 | Fall 2026 |
| 20261 | Spring 2026 |
| 20256 | Summer 2026 |
| 20253 | Fall 2025 |
| 20251 | Spring 2025 |
