# TrojanScheduler — Cloud Deployment Guide

Deploy the app so anyone can use it at a public URL — no installation needed.

**Recommended stack:**

| Layer | Service | Cost |
|-------|---------|------|
| **Frontend** | [Netlify](https://www.netlify.com) | Free |
| **Backend** | [Railway](https://railway.app) | ~Free for low traffic |
| **Database** | Railway MySQL plugin | Included |

**Total cost:** Free for personal / low-traffic use.

**Alternative:** If Railway doesn’t work for you, use [Render](https://render.com) for the backend + PostgreSQL (see **Alternative — Deploy the backend on Render** below).

**Time to deploy:** ~20 minutes the first time.

---

## Overview

```
Browser  ──HTTPS──▶  Netlify (React app)
                          │  HTTPS API calls
                          ▼
                     Railway (Spring Boot)
                          │  JDBC
                          ▼
                     Railway MySQL
```

---

## Step 1 — Push your code to GitHub

Railway and Netlify both deploy directly from GitHub. If you haven't already:

```bash
# From the project root
git remote add origin https://github.com/YOUR_USERNAME/trojanscheduler.git
git push -u origin main
```

Make sure the repo contains at least:
- `backend/` with `Dockerfile`
- `frontend/` with `netlify.toml`

---

## Step 2 — Deploy the backend on Railway

### 2a. Create a Railway account

Go to [railway.app](c) → sign up with GitHub (recommended — it links your repos automatically).

### 2b. Create a new project

1. Click **New Project**
2. Choose **Deploy from GitHub repo**
3. Select your `trojanscheduler` repository
4. **Set the root directory** so the backend builds correctly:
   - Click your **backend service** → **Settings** → **General**
   - Set **Root Directory** to `backend` (so Railway uses `backend/Dockerfile` and the correct `pom.xml` / `src`)
   - If you prefer to build from the repo root, leave Root Directory empty — the repo has a root `Dockerfile` that builds the backend from `backend/`
5. Railway will detect the Dockerfile and start building. The first build takes ~3–5 minutes (Maven downloads dependencies).

### 2c. Add a MySQL database

Inside your Railway project:

1. Click **+ New** → **Database** → **MySQL**
2. Railway provisions a MySQL 8 instance and wires it into your project automatically

### 2d. Set environment variables

In the Railway dashboard, click your **backend service** → **Variables** tab → add these:

| Variable | Value |
|----------|-------|
| `DB_URL` | Copy from the MySQL plugin's **MYSQL_URL** variable, but replace the scheme: `jdbc:mysql://...` (see note below) |
| `DB_USERNAME` | Copy from MySQL plugin → `MYSQLUSER` |
| `DB_PASSWORD` | Copy from MySQL plugin → `MYSQLPASSWORD` |
| `JWT_SECRET` | A random string, 32+ characters. Generate one: `openssl rand -base64 32` |
| `ALLOWED_ORIGINS` | Leave blank for now — you'll add your Netlify URL in Step 3 |
| `REFRESH_COOKIE_SECURE` | `true` |
| `REFRESH_COOKIE_SAMESITE` | `None` |

> **DB_URL note:** Railway's MySQL plugin gives you a URL like `mysql://user:pass@host:port/db`. You need to prefix it with `jdbc:` and add parameters:
> ```
> jdbc:mysql://HOST:PORT/DB_NAME?useSSL=true&serverTimezone=UTC
> ```
> Copy the host, port, and database name from the plugin's connection details tab.

### 2e. Get your backend URL

Once deployed, Railway gives you a public URL like:
```
https://trojanscheduler-production-xxxx.up.railway.app
```

Find it under your backend service → **Settings** → **Networking** → **Public Networking** → click **Generate Domain** if none exists.

Copy this URL — you need it for Step 3.

---

## Alternative — Deploy the backend on Render (if Railway isn’t working)

Render has a free tier and a **free PostgreSQL** database. The app supports both MySQL (Railway) and PostgreSQL (Render).

### Render 1. Create account and project

1. Go to [render.com](https://render.com) → sign up with GitHub.
2. **Dashboard** → **New +** → **Web Service**.
3. Connect your GitHub repo (`trojanscheduler` or `USC-Scheduler`).
4. Configure:
   - **Name:** e.g. `trojanscheduler-api`
   - **Region:** pick one close to you
   - **Root Directory:** `backend` (so Render uses `backend/Dockerfile`)
   - **Runtime:** **Docker**
   - **Instance type:** **Free**

### Render 2. Add PostgreSQL database

1. In the same Render dashboard, **New +** → **PostgreSQL**.
2. Name it (e.g. `trojanscheduler-db`), choose **Free** plan, create.
3. Open the new database → **Info** tab. Note:
   - **Internal Database URL** (use this so the app and DB talk inside Render)
   - Or **Host**, **Database**, **Username**, **Password**, **Port** (5432)

### Render 3. Set environment variables

In your **Web Service** (backend) → **Environment** tab, add:

| Key | Value |
|-----|--------|
| `SPRING_PROFILES_ACTIVE` | `postgres` |
| `DB_URL` | `jdbc:postgresql://HOST:5432/DATABASE?sslmode=require` — replace HOST and DATABASE with the values from the PostgreSQL service (Internal Host and Database name). Example: `jdbc:postgresql://dpg-xxxx.oregon-postgres.render.com:5432/dbname?sslmode=require` |
| `DB_USERNAME` | PostgreSQL **Username** from the DB Info tab |
| `DB_PASSWORD` | PostgreSQL **Password** from the DB Info tab |
| `JWT_SECRET` | A random 32+ character string (e.g. `openssl rand -base64 32`) |
| `REFRESH_COOKIE_SECURE` | `true` |
| `REFRESH_COOKIE_SAMESITE` | `None` |

Use the **Internal** connection details (not External) so the app and DB are on the same network. If you only see one URL, use it to build `DB_URL`:  
`jdbc:postgresql://<host>:5432/<database>?sslmode=require` and the same user/password from that URL.

### Render 4. Health check (optional but recommended)

In your Web Service → **Settings** → **Health Check Path**, set:

```
/actuator/health
```

So Render knows the app is up (Spring Boot exposes this endpoint).

### Render 5. Deploy and get URL

1. Click **Create Web Service**. Render builds the Docker image and starts the app (~3–5 min first time).
2. Once live, copy the service URL (e.g. `https://trojanscheduler-api.onrender.com`).
3. Use this URL as your backend URL for the frontend (Step 3) and for CORS (Step 4).

**Note:** On the free tier, the service sleeps after ~15 minutes of no traffic. The first request after that may take 30–60 seconds to wake up.

---

## Step 3 — Deploy the frontend on Netlify

### 3a. Create a Netlify account

Go to [netlify.com](https://www.netlify.com) → sign up with GitHub.

### 3b. Import your repository

1. **Add new site** → **Import an existing project**
2. Choose **GitHub** and authorize; select your repository
3. Configure build settings:

| Setting | Value |
|---------|-------|
| **Branch to deploy** | `main` (or your default) |
| **Base directory** | `frontend` |
| **Build command** | `npm run build` |
| **Publish directory** | `dist` |

(`frontend/netlify.toml` sets these; Netlify may pick them up once Base directory is `frontend`.)

### 3c. Set environment variables

**Site settings** → **Environment variables** → **Add a variable** (or **Add from .env**):

| Key | Value |
|-----|--------|
| `VITE_API_URL` | Your Railway backend URL from Step 2e (e.g. `https://trojanscheduler-production-xxxx.up.railway.app`) |

No trailing slash. For production, use the same backend URL the browser will call.

### 3d. Deploy

Click **Deploy site**. Netlify builds and deploys in ~1 minute.

Your app is live at a URL like:
```
https://random-name-12345.netlify.app
```

You can change the site name under **Site settings** → **Domain management** → **Edit site name**.

---

## Step 4 — Connect them together (CORS)

The backend must allow your Netlify frontend origin.

- **Railway:** Backend service → **Variables** → add or edit:  
  `ALLOWED_ORIGINS` = `https://your-site-name.netlify.app,http://localhost:5173`
- **Render:** Backend service → **Environment** → add or edit:  
  `ALLOWED_ORIGINS` = `https://your-site-name.netlify.app,http://localhost:5173`

Use your real Netlify URL (no trailing slash). Keep `http://localhost:5173` for local dev. Save; the backend will redeploy.

---

## Step 5 — Verify everything works

1. Open your Netlify URL in a browser
2. Register a new account
3. Search for a course (`CSCI`, term `20263`)
4. Add a section to your schedule → should appear on the calendar
5. Add a section to your watchlist
6. Open the app on your phone → login works, watchlist shows the same item, schedule is empty (localStorage is per-device)

---

## Redeployment (ongoing)

Every time you push to `main`:
- **Netlify** redeploys the frontend automatically (~1 min)
- **Railway** redeploys the backend automatically (~3 min)

No manual steps needed.

---

## Custom domain (optional)

### Netlify
Site settings → Domain management → Add custom domain → follow the DNS instructions (e.g. CNAME or A record).

### Railway
Backend service → Settings → Networking → Custom Domain → add your subdomain (e.g. `api.yourdomain.com`) → add a CNAME record in your DNS provider pointing to Railway's domain.

Then update `VITE_API_URL` in Netlify and `ALLOWED_ORIGINS` in Railway to match.

---

## Troubleshooting

**Backend build fails on Railway**

Check the build logs in Railway → your service → **Deployments** → click the failed build → **View logs**.

Common causes:

| Symptom | Fix |
|--------|-----|
| `COPY failed: file not found`, `pom.xml not found`, or `src` not found | Set **Root Directory** to `backend`: Service → **Settings** → **General** → **Root Directory** = `backend`. Then redeploy. |
| Maven dependency download timeout | Redeploy; Railway will retry. If it keeps failing, try again in a few minutes (network blip). |
| Out of memory during build | Railway’s default builder has limited memory. Set **Root Directory** to `backend` so only that folder is in the build context, and redeploy. |
| Java version mismatch | The Dockerfiles use `eclipse-temurin:21`; the project requires Java 21. No change needed unless you edited the Dockerfile. |

If it still fails, copy the **last 30–50 lines** of the build log (the red error part) and use that to debug (e.g. missing env var, or a specific Maven/Flyway error).

**"CORS error" in browser console**

`ALLOWED_ORIGINS` in Railway doesn't include your Netlify URL. Add it exactly (no trailing slash) and redeploy the backend.

**Login works but refresh fails (401 loop)**

`REFRESH_COOKIE_SECURE` must be `true` and `REFRESH_COOKIE_SAMESITE` must be `None` when frontend and backend are on different domains (Netlify vs Railway). These are required for cross-site cookies over HTTPS.

**WebSocket notifications not working**

Railway supports WebSockets by default. If you added a custom domain, make sure your DNS/proxy (e.g. Cloudflare) has WebSocket proxying enabled.

**Railway free tier limit**

Railway's free tier includes $5 of credit per month. A small Spring Boot app + MySQL typically uses ~$2–3/month. If you exceed it, Railway pauses the service. Upgrade to the Hobby plan ($5/month) for uninterrupted uptime.

---

## Local Docker testing (optional)

Before deploying, you can test the full Docker setup locally:

```bash
# From the project root
DB_PASSWORD=testpass JWT_SECRET=dev-change-me-dev-change-me-dev docker compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- MySQL: localhost:3306

To stop: `Ctrl+C` then `docker compose down`.

To wipe the database and start fresh: `docker compose down -v`.
