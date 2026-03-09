# Run and test the full stack

Use this guide to run the backend and frontend together and verify the main flows.

---

## 1. Prerequisites

Install the following (if not already installed):

| Tool   | Version  | Check command   |
|--------|----------|-----------------|
| Java   | 21       | `java -version` |
| Maven  | 3.6+     | `./backend/mvnw -v` (or system `mvn -v`) |
| Node.js| 18+      | `node -v`       |
| npm    | 9+       | `npm -v`        |
| MySQL  | 8.x      | `mysql --version` |

---

## 2. MySQL setup

The backend expects a MySQL database. Default config (see `backend/src/main/resources/application.properties`):

- **URL:** `jdbc:mysql://localhost:3306/trojanscheduler?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
- **User:** `root`
- **Password:** *(empty)*

You can override with env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

**How to do it (quick path):**

1. **Install MySQL** (if needed): on macOS with Homebrew run `brew install mysql`; on Windows use the [MySQL installer](https://dev.mysql.com/downloads/installer/).
2. **Start MySQL:**  
   - macOS (Homebrew): `brew services start mysql`  
   - Windows: start the “MySQL” service in Services or from the MySQL installer.  
   - Linux: `sudo systemctl start mysql`
3. **Use the default connection (easiest):** If your MySQL `root` user has **no password**, do nothing else. When you start the backend (Step 3 below), it will create the `trojanscheduler` database and tables automatically.
4. **If `root` has a password:** When starting the backend, set it:  
   `DB_PASSWORD=your_mysql_root_password ./mvnw spring-boot:run`  
   (run that from the `backend` folder.)

**Option A – Use defaults (empty root password)**  
Ensure MySQL is running. The app will create the database `trojanscheduler` on first run (Flyway will create tables).

**Option B – Custom database/user**  
Create the database and a dedicated user, then set env before starting the backend.

**How to create the database and user**

1. **Open MySQL.** Use either:
   - **Terminal:** Log in as `root` (use your MySQL root password if you set one):
     ```bash
     mysql -u root -p
     ```
     At the prompt, type your root password and press Enter. You’ll see a `mysql>` prompt.
   - **MySQL Workbench:** Connect to your local MySQL server (e.g. “Local instance MySQL”) using root (or an admin user).

2. **Run these SQL commands** (copy and paste, then press Enter). Replace `your_password` with a real password you’ll use for the app:
   ```sql
   CREATE DATABASE IF NOT EXISTS trojanscheduler;
   CREATE USER IF NOT EXISTS 'tsuser'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL ON trojanscheduler.* TO 'tsuser'@'localhost';
   FLUSH PRIVILEGES;
   ```
   - `CREATE DATABASE` creates the database the backend will use.
   - `CREATE USER` creates a user named `tsuser` that can connect from this machine.
   - `GRANT ALL` gives that user full access to the `trojanscheduler` database (and its tables).
   - `FLUSH PRIVILEGES` makes MySQL apply the new permissions.

3. **Exit MySQL:** Type `exit` and press Enter (terminal), or close the query tab (Workbench).

4. **Tell the backend to use this user.** Before starting the backend, set these env vars (use the same password you put in the SQL above):
   ```bash
   export DB_URL="jdbc:mysql://localhost:3306/trojanscheduler?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   export DB_USERNAME=tsuser
   export DB_PASSWORD=your_password
   ```
   Then start the backend (see Step 3 below). On Windows (Command Prompt) use `set DB_USERNAME=tsuser` etc. instead of `export`.

**Start MySQL (if not running):**

- **macOS (Homebrew):** `brew services start mysql`
- **Windows:** Start the MySQL service from Services or MySQL installer.
- **Linux:** `sudo systemctl start mysql` (or `mariadb`).

---

## 3. Start the backend

From the project root:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows: `mvnw.cmd spring-boot:run` (instead of `./mvnw`).

Wait until you see something like:

```text
Started BackendApplication in X.XXX seconds
```

Backend will be at **http://localhost:8080**.

**Quick health check:**

```bash
curl -s http://localhost:8080/actuator/health
```

You should get JSON with `"status":"UP"`.

**Port 8080 already in use**

If you see *"Web server failed to start. Port 8080 was already in use"*:

1. **Find what’s using 8080:**  
   `lsof -i :8080`  
   Note the **PID** (second column; e.g. `90485`).

2. **Stop that process:**  
   `kill <PID>`  
   Example: `kill 90485`. If it doesn’t exit, use `kill -9 <PID>`.

3. Start the backend again: `./mvnw spring-boot:run`.

**Or use another port:**  
`SERVER_PORT=8081 ./mvnw spring-boot:run`  
Then in the frontend set `VITE_API_URL=http://localhost:8081` in `.env` (or create it from `.env.example`).

---

**If Flyway fails** (e.g. “Migration checksum mismatch”):  
Your DB may have been changed manually. For a clean dev DB you can drop and recreate the database and run again; Flyway will re-apply all migrations.

---

## 4. Start the frontend

Open a **second terminal**. From the project root:

```bash
cd frontend
```

Create env file (only needed if backend is not at `http://localhost:8080`):

```bash
cp .env.example .env
# Edit .env and set VITE_API_URL if needed (default is http://localhost:8080)
```

Install dependencies (if not already done) and run the dev server:

```bash
npm install
npm run dev
```

You should see something like:

```text
  VITE v5.x.x  ready in XXX ms
  ➜  Local:   http://localhost:5173/
```

Frontend is at **http://localhost:5173**.

---

## 5. Test the full stack

Use the browser at **http://localhost:5173**. Follow these steps to test the main flows.

### 5.1 Auth

1. **Register**
   - Click “Register” (or go to `/register`).
   - Enter email and password (e.g. `test@example.com` / `password123`).
   - Submit → you should be logged in and redirected (e.g. to Search).

2. **Log out**
   - Click “Log out” in the header → you should land on the login page.

3. **Log in**
   - Enter the same email and password.
   - Submit → you should be logged in again.

### 5.2 Course search

1. Go to **Search** (or `/search`).
2. Leave or set **Term** (e.g. `20263`).
3. Enter a **Search** term (e.g. `MATH` or `CSCI`).
4. Click **Search**.
5. You should see a list of courses/sections (from the USC API via the backend). If the USC API is slow or down, you may see an error or empty list; that’s expected when the external API fails.

### 5.3 Watchlist

1. On Search results, click **“Add to watchlist”** on a section.
2. You should be redirected to **Watchlist** and see that section listed.
3. Click **“Remove”** on the same item → it should disappear from the list.

### 5.4 Schedule and conflict check

1. Go back to **Search** and run a search again.
2. Click **“Add to schedule”** on one section.
3. You should land on **Schedule** and see that section added (no conflict).
4. Go back to **Search**, pick **another section**, and click **“Add to schedule”**.
   - If the two sections **overlap in time on the same day**, you should see a **“Schedule conflict”** message and the second section should **not** be added.
   - If they don’t overlap, the second section should be added.
5. On Schedule, click **“Remove”** on a section → it should disappear. Schedule is stored in `localStorage` (per browser).

### 5.5 WebSocket notifications (optional)

Watchlist “seat opened” alerts are sent over WebSocket. To see them:

1. Add a section to the **watchlist** that is currently **full** (so the backend can later see a seat open).
2. Keep the **Watchlist** page open.
3. When the backend polling runs and detects a seat opening for that section, a green alert banner should appear on the Watchlist page.

Polling runs on an interval (see `app.polling.interval-ms` in `application.properties`; default is long). For faster testing you can temporarily reduce that value and restart the backend.

---

## 6. Optional: API checks with curl

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

Response includes `accessToken`. Use it for protected endpoints:

```bash
# Replace YOUR_ACCESS_TOKEN with the token from the login response
curl -s http://localhost:8080/api/watchlist \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -b cookies.txt
```

**Conflict check (requires a valid token):**

```bash
curl -s -X POST http://localhost:8080/api/schedule/check-conflict \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "existingMeetings": [{"dayCode":"MW","startTime":"14:00","endTime":"15:20"}],
    "candidateMeetings": [{"dayCode":"MW","startTime":"15:00","endTime":"16:20"}]
  }'
```

You should get JSON with `conflict: true` (overlap on Monday/Wednesday 15:00–15:20).

---

## 7. Troubleshooting

| Issue | What to check |
|-------|----------------|
| Backend won’t start | Java 21 installed? MySQL running? `DB_URL` / user / password correct? Port 8080 free? |
| **“Access denied for user 'root'@'localhost' (using password: YES)”** | You set `DB_PASSWORD` but MySQL `root` has a different password or no password. If root has no password, run without it: `./mvnw spring-boot:run`. If root has a password, use that: `DB_PASSWORD=your_actual_mysql_password ./mvnw spring-boot:run`. |
| **“Port 8080 was already in use”** | Another process (often a previous backend) is using 8080. Stop it: `lsof -i :8080` to see the PID, then `kill <PID>`. Or run on another port: `SERVER_PORT=8081 ./mvnw spring-boot:run` (and use `VITE_API_URL=http://localhost:8081` in the frontend). |
| **“Failed to fetch” on login/register** | Backend is not reachable. Start it first: `cd backend && ./mvnw spring-boot:run`. Use the app at http://localhost:5173 (not 127.0.0.1). |
| Flyway / schema errors | DB in a bad state? Try dropping `trojanscheduler` and starting again so Flyway can run from scratch. |
| Frontend “Loading…” or auth errors | Backend running on 8080? CORS allows `http://localhost:5173` (see `SecurityConfig`). |
| Search returns nothing / error | Backend can reach `https://classes.usc.edu`; network or USC API may be down or slow. |
| WebSocket not connecting | Backend running; browser devtools → Network → WS; check for 403/401 (JWT on CONNECT). |

---

## 8. Summary

| Step | Command / URL |
|------|----------------|
| 1. Start MySQL | `brew services start mysql` (or your OS’s method) |
| 2. Start backend | `cd backend && ./mvnw spring-boot:run` → http://localhost:8080 |
| 3. Start frontend | `cd frontend && npm install && npm run dev` → http://localhost:5173 |
| 4. Test in browser | http://localhost:5173 — Register, Login, Search, Watchlist, Schedule |

Once both servers are running, use the **Test the full stack** section above to verify auth, search, watchlist, schedule, and conflict detection.

**To stop everything:** In the terminal where the backend is running, press **Ctrl+C**. In the terminal where the frontend is running, press **Ctrl+C**. (To stop MySQL as well: `brew services stop mysql` on macOS, or stop the MySQL service on Windows/Linux.)
