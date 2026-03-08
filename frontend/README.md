# Trojan Scheduler — Frontend

React (Vite + TypeScript) app for course search, watchlist, schedule builder, and real-time seat alerts.

## Setup

```bash
cp .env.example .env
# Edit .env if your backend is not at http://localhost:8080

npm install
npm run dev
```

Open http://localhost:5173. Ensure the backend is running and CORS allows `http://localhost:5173`.

## Features

- **Auth**: Register, login, JWT + refresh cookie
- **Search**: Courses by term and query (USC API); add to watchlist or schedule
- **Watchlist**: List/remove watched sections; real-time “seat opened” alerts via WebSocket
- **Schedule**: Add sections from search; conflict check before add; list/remove (stored in `localStorage`)

## Build

```bash
npm run build
npm run preview
```
