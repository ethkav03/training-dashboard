# Momentum

A personal training & performance dashboard — weight, nutrition, training,
recovery and goals unified into one daily view. See
[`docs/`](./docs/README.md) for architecture, data model, and the exact
formula behind every computed value (readiness score, training load,
estimated 1RM, goal progress, insights, etc.), and
[`docs/roadmap.md`](./docs/roadmap.md) for what's built vs. deliberately out
of scope for this pass.

## Stack

React + Vite + TypeScript + Tailwind (frontend) · Express + TypeScript +
Prisma (backend) · PostgreSQL · Docker Compose for local dev.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (running)
- Node.js 20+ and npm (only needed for `npm install` and running Prisma
  commands from the host — the app itself runs inside containers)

## Quick start

```bash
npm install
docker compose up -d --build
```

That's it — this brings up Postgres, the backend (`:4000`), and the frontend
(`:5173`) as three containers on one Docker network, with source bind-mounted
for hot reload on both sides. First boot runs the Prisma migrations
automatically.

Open **http://localhost:5173** and click **"Continue as Dev User (local
only)"** to get in immediately — no Google credentials needed to explore the
app. See [Google OAuth setup](#google-oauth-setup-optional) below to enable
real sign-in.

Useful commands (see root `package.json` for the full list):

```bash
npm run docker:logs     # tail backend + frontend logs
npm run docker:down     # stop everything
npm run prisma:studio   # open Prisma Studio against the running DB
```

## Running without Docker for the app (Postgres still via Docker)

```bash
npm install
docker compose up -d postgres      # just the database, on host port 5434
cd backend && npx prisma migrate deploy && cd ..
npm run dev                        # runs backend + frontend directly on the host
```

## Environment variables

`backend/.env` and `frontend/.env` are gitignored; `.env.example` in each
folder documents every key. Sensible local defaults are already in place for
`DATABASE_URL`, `PORT`, and `JWT_SECRET` — the only thing you need to add
yourself is Google OAuth credentials, and only if you want real Google
sign-in instead of the dev-login bypass.

## Google OAuth setup (optional)

The dev-login bypass works with zero configuration. To enable real "Continue
with Google":

1. [Google Cloud Console](https://console.cloud.google.com/) → new project.
2. **APIs & Services → OAuth consent screen** → User type **External** → add
   your own Google account as a test user (stays in "Testing" mode, no app
   verification needed for personal use).
3. **APIs & Services → Credentials → Create Credentials → OAuth Client ID**
   → **Web application**:
   - Authorized JavaScript origin: `http://localhost:5173`
   - Authorized redirect URI: `http://localhost:4000/api/auth/google/callback`
4. Copy the Client ID/Secret into `backend/.env` as `GOOGLE_CLIENT_ID` /
   `GOOGLE_CLIENT_SECRET`, then `docker compose up -d --build backend` (or
   restart the backend if running on the host) to pick them up.

## Native Android app (optional, in progress)

`android/` is a separate Kotlin/Gradle project (Health Connect integration —
see [`docs/architecture.md`](./docs/architecture.md#native-android-app) for
why it exists) — **not** part of this repo's npm workspaces. Open it directly
in Android Studio, not via this README's commands:

1. Android Studio → Open → select the `android/` folder.
2. Copy `android/local.properties.example` to `android/local.properties` and
   fill in `GOOGLE_WEB_CLIENT_ID` (same value as `backend/.env`'s
   `GOOGLE_CLIENT_ID`) — see the architecture doc for the extra Google Cloud
   Console step this needs (a separate Android-type OAuth client).
3. Run on an emulator or device. The default `API_BASE_URL` targets the
   standard emulator; see the architecture doc for real-device networking.

This part of the project has been written but not yet compiled/run in this
environment (no Android SDK or emulator available where it was built) —
Android Studio's build is the first real verification step.

## Notes for local development

- Postgres runs on host port **5434**, not 5432 — a different Postgres
  service may already be using 5432 on your machine. Inside the Docker
  network the backend reaches it on the standard port via the `postgres`
  service name; only the host-facing port is remapped.
- Both containers hot-reload on source changes (Vite for the frontend,
  `nodemon` for the backend) — no rebuild needed for code edits, only for
  dependency changes (`docker compose up -d --build`).
