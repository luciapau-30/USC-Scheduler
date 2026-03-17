# Run and test the full stack (Netlify)

This guide covers running the app locally and deploying the frontend to **Netlify**. The backend runs locally or on Railway; the frontend is served from Netlify in production.

---

## 1. Prerequisites

Install the following (if not already installed):

| Tool    | Version | Check command                    |
|---------|---------|-----------------------------------|
| Java    | 21      | `java -version`                   |
| Maven   | 3.6+    | `./backend/mvnw -v` or `mvn -v`  |
| Node.js | 18+     | `node -v`                         |
| npm     | 9+      | `npm -v`                          |
| MySQL   | 8.x     | `mysql --version` (for local run) |

---

## 2. MySQL setup (for local backend)

The backend expects a MySQL database when running locally. Default (see `backend/src/main/resources/application.properties`):

- **URL:** `jdbc:mysql://localhost:3306/trojanscheduler?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
- **User:** `root`
- **Password:** *(empty)*

Override with env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

**Quick path:**

1. **Install MySQL** (if needed): macOS `brew install mysql`; Windows use [MySQL installer](https://dev.mysql.com/downloads/installer/).
2. **Start MySQL:**  
   - macOS (Homebrew): `brew services start mysql`  
   - Windows: start the MySQL service in Services.  
   - Linux: `sudo systemctl start mysql`
3. **Default (no root password):** Do nothing else; the backend will create the `trojanscheduler` database and tables on first run.
4. **If root has a password:** When starting the backend:  
   `DB_PASSWORD=your_mysql_root_password ./mvnw spring-boot:run` (from the `backend` folder).

**Custom database/user:** Create the DB and user in MySQL, then set env before starting the backend:

```bash
export DB_URL="jdbc:mysql://localhost:3306/trojanscheduler?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME=tsuser
export DB_PASSWORD=your_password
```

(SQL to run in MySQL: `CREATE DATABASE trojanscheduler;` then create user and grant privileges.)

---

## 3. Start the backend (local)

From the project root:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows: `mvnw.cmd spring-boot:run`.

Wait for: `Started BackendApplication in X.XXX seconds`. Backend is at **http://localhost:8080**.

**Health check:**

```bash
curl -s http://localhost:8080/actuator/health
```

You should get JSON with `"status":"UP"`.

**Port 8080 already in use**

- Find process: `lsof -i :8080` (note the PID).
- Stop it: `kill <PID>` (or `kill -9 <PID>`).
- Or use another port: `SERVER_PORT=8081 ./mvnw spring-boot:run`, then set `VITE_API_URL=http://localhost:8081` in the frontend `.env`.

---

## 4. Start the frontend (local)

Open a **second terminal**. From the project root:

```bash
cd frontend
cp .env.example .env
# Edit .env if backend is not on 8080: VITE_API_URL=http://localhost:8081
npm install
npm run dev
```

Frontend is at **http://localhost:5173**.

---

## 5. Test the full stack locally

In the browser at **http://localhost:5173**:

1. **Auth:** Register → Log out → Log in.
2. **Search:** Go to Search, set term (e.g. `20263`), enter subject (e.g. `MATH` or `CSCI`), click Search. You should see courses from the USC API via the backend.
3. **Watchlist:** Add a section to the watchlist from Search, then open Watchlist and remove it.
4. **Schedule:** Add a section to the schedule from Search; add another that overlaps in time to see the conflict message. Remove sections from Schedule.
5. **WebSocket (optional):** Add a full section to the watchlist, keep the Watchlist page open; when the backend detects a seat open, a notification can appear (polling interval is long by default).

---

## 6. Deploy the frontend to Netlify

### 6a. Push code to GitHub

Ensure your repo is on GitHub (e.g. `https://github.com/YOUR_USERNAME/USC-Scheduler`).

### 6b. Create a Netlify account and site

1. Go to [netlify.com](https://www.netlify.com) → sign up with GitHub.
2. **Add new site** → **Import an existing project**.
3. Choose **GitHub** and authorize; select your repository.
4. Configure build settings:

| Setting            | Value        |
|--------------------|-------------|
| **Branch to deploy** | `main` (or your default) |
| **Base directory**   | `frontend`  |
| **Build command**    | `npm run build` |
| **Publish directory**| `dist`      |

These are also set in `frontend/netlify.toml`, so Netlify may pick them up automatically once Base directory is `frontend`.

### 6c. Environment variables

In Netlify: **Site settings** → **Environment variables** → **Add a variable** (or **Add from .env**).

| Key             | Value |
|-----------------|--------|
| `VITE_API_URL`  | Your backend URL: `http://localhost:8080` for local, or your **production** backend URL (e.g. `https://your-backend.onrender.com` or `https://your-backend.up.railway.app`). |

For production, use the same backend URL the browser will call (Render, Railway, etc.). No trailing slash.

**Redeploy after changing env vars:** **Deploys** → **Trigger deploy** → **Clear cache and deploy site**.

### 6d. Get your frontend URL

After the first deploy, Netlify gives a URL like `https://random-name-12345.netlify.app`. You can change it under **Site settings** → **Domain management** → **Edit site name**.

### 6e. Connect backend and frontend (CORS)

Your backend must allow the Netlify origin. In your backend (Render or Railway) **Environment / Variables**:

- **ALLOWED_ORIGINS** = `https://your-site-name.netlify.app,http://localhost:5173`

Use your real Netlify URL (no trailing slash). Keep `http://localhost:5173` for local dev. Save so the backend redeploys.

---

## 7. Test the deployed app (Netlify)

1. Open your Netlify URL (e.g. `https://your-site-name.netlify.app`).
2. Register and log in (requests go to the backend URL you set in `VITE_API_URL`).
3. Run through Search, Watchlist, and Schedule. If `VITE_API_URL` points to a production backend, everything should work the same as locally (except the backend is remote).

If you see “Failed to fetch” or CORS errors, check:

- `VITE_API_URL` in Netlify matches the backend the browser can reach.
- Backend `ALLOWED_ORIGINS` includes your Netlify URL exactly.

---

## 8. Optional: API checks with curl

**Register:**

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"curl@test.com","password":"password123"}' \
  -c cookies.txt
```

**Login:**

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"curl@test.com","password":"password123"}' \
  -c cookies.txt -b cookies.txt
```

Use the `accessToken` from the response for protected endpoints:

```bash
curl -s http://localhost:8080/api/watchlist \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -b cookies.txt
```

---

## 9. Troubleshooting

| Issue | What to check |
|-------|----------------|
| **MySQL: "Bootstrap failed: 5" / launchctl error** | See **MySQL won't start (macOS)** below. |
| Backend won’t start | Java 21, MySQL running, correct `DB_*` env vars, port 8080 free. |
| “Access denied” (MySQL) | `DB_PASSWORD` matches your MySQL user, or omit it if root has no password. |
| Port 8080 in use | `lsof -i :8080` → `kill <PID>`, or use `SERVER_PORT=8081` and set `VITE_API_URL` in frontend. |
| “Failed to fetch” on login/register | Backend running; use http://localhost:5173 (not 127.0.0.1). For Netlify, check `VITE_API_URL` and backend is reachable. |
| Flyway / schema errors | DB in bad state; consider dropping and recreating the DB so Flyway can re-run migrations. |
| Frontend “Loading…” or auth errors | Backend up; CORS includes your frontend origin (localhost:5173 or Netlify URL). |
| Search returns nothing / error | Backend can reach `https://classes.usc.edu`; network or USC API may be down/slow. |
| Netlify build fails | Base directory = `frontend`, build command = `npm run build`, publish = `dist`. Check build logs. |
| Netlify app can't reach backend | `VITE_API_URL` set in Netlify env; backend `ALLOWED_ORIGINS` includes the Netlify URL. Redeploy both after changes. |

**MySQL won't start (macOS) — "Bootstrap failed: 5" / Input-output error**

This often means the launchd job is stuck or already loaded. Try in order:

1. **Unload then start again**
   ```bash
   brew services stop mysql
   brew services start mysql
   ```

2. **If that fails, unload the plist manually and restart**
   ```bash
   launchctl unload ~/Library/LaunchAgents/homebrew.mxcl.mysql.plist
   brew services start mysql
   ```

3. **Run MySQL in the foreground (no launchd)**  
   In a terminal, run:
   ```bash
   mysql.server start
   ```
   Or: `brew run mysql` (keeps running until you Ctrl+C). Use another terminal for the backend.

4. **Check if MySQL is already running**  
   If `mysql -u root` (or `mysql -u root -p`) connects, MySQL is up and you can skip `brew services start` and just run the backend.

5. **Restart Mac**  
   Sometimes launchd needs a reboot after permission or path changes.

---

## 10. Summary

**Local run**

| Step | Command / URL |
|------|----------------|
| 1. Start MySQL | `brew services start mysql` (or your OS method) |
| 2. Start backend | `cd backend && ./mvnw spring-boot:run` → http://localhost:8080 |
| 3. Start frontend | `cd frontend && npm install && npm run dev` → http://localhost:5173 |
| 4. Test | http://localhost:5173 — Register, Login, Search, Watchlist, Schedule |

**Deploy frontend to Netlify**

- Connect GitHub repo, set **Base directory** = `frontend`, **Build** = `npm run build`, **Publish** = `dist`.
- Set **VITE_API_URL** to your backend URL (local or production).
- Set backend **ALLOWED_ORIGINS** to your Netlify URL and `http://localhost:5173`.

**Stop everything (local):** Ctrl+C in the backend and frontend terminals. To stop MySQL: `brew services stop mysql` (macOS) or stop the MySQL service (Windows/Linux).
