# Architecture

## Stack

| Layer | Choice |
|---|---|
| Frontend | React 18 + TypeScript, Vite, Tailwind CSS, TanStack Query, React Router, React Hook Form, Recharts |
| Backend | Node.js + Express + TypeScript, Prisma ORM |
| Database | PostgreSQL 16 |
| Auth | Google OAuth 2.0 (Passport) + backend-issued JWT |
| Dev environment | Docker Compose (three sibling containers: postgres, backend, frontend) |

Frontend and backend are separate npm workspaces, not a single full-stack
framework — chosen so the backend can serve a future native mobile client on
the same REST API without change.

## Monorepo layout

```
training-dashboard/
├── docker-compose.yml        # postgres + backend + frontend, one project
├── packages/shared/          # enums + DTOs shared by both apps, no build step
│   └── src/{enums.ts,dto.ts}
├── backend/
│   ├── prisma/schema.prisma
│   └── src/
│       ├── routes/            # thin, one file per resource
│       ├── services/          # all business logic — see calculations.md
│       ├── validation/        # zod schemas per route
│       ├── middleware/        # requireAuth, validate, errorHandler
│       └── config/            # env loader, passport strategy
└── frontend/
    └── src/
        ├── pages/              # one per route, + progress/ and training/ sub-pages
        ├── components/
        │   ├── ui/             # Button, Card, Modal — generic primitives
        │   ├── layout/         # AppShell, TopNav, BottomNav, ProtectedRoute
        │   ├── cards/          # ReadinessBadge, GoalCard, InsightCard
        │   ├── charts/         # ChartCard wrapper, WeightTrendChart, ExerciseProgressionChart
        │   ├── forms/          # one modal-form component per entity
        │   └── timeline/
        ├── api/                # typed axios wrappers, one file per resource
        ├── hooks/              # TanStack Query hooks, one file per resource
        └── context/AuthContext.tsx
android/                        # standalone Kotlin/Gradle project -- NOT an npm
└── app/src/main/kotlin/...     # workspace, opened separately in Android Studio;
                                 # see "Native Android app" below
```

`packages/shared` holds enums (`ActivityType`, `GoalStatus`, etc.) and DTO
interfaces (`WeightEntryDto`, `DashboardTodayDto`, ...) consumed as source by
both apps via an npm workspace link — no build step, no drift between what the
backend sends and what the frontend expects.

## Request flow

Every route is `router -> validateBody/validateQuery (zod) -> service function
-> Prisma -> DTO mapper -> res.json`. Routes never contain business logic —
every calculation, every multi-step write, every "what does this record mean"
decision lives in `src/services/*.ts`, one file per pillar plus
`dashboardService`, `timelineService`, `insightsService`, `gamificationService`
for the cross-cutting composition endpoints. This is why `dashboardService.ts`
can compose `/dashboard/today` out of the exact same functions
`/weight/trend`, `/nutrition/summary`, etc. call directly — the numbers can't
drift between the Today page and the detail pages because there's only one
implementation of each calculation.

## Auth flow

Google OAuth is handled entirely server-side; the two apps don't share a
session — the backend issues its own JWT after a successful OAuth exchange.

1. Frontend "Continue with Google" does a full-page navigation to
   `GET /api/auth/google`.
2. Express (via `passport-google-oauth20`) redirects to Google's consent screen.
3. Google redirects back to `GET /api/auth/google/callback?code=...`.
4. The Passport verify callback calls `userService.upsertUserFromGoogleIdentity()`
   — first login creates the user, subsequent logins just refresh
   name/email/avatar. This is factored out as a shared function (rather than
   upserting inline in `passport.ts`) so a future mobile Google Sign-In flow
   can reuse the exact same upsert instead of a second, slightly different one.
5. The route handler signs a JWT (`{ sub: userId, email }`, `JWT_SECRET`,
   7-day expiry by default) and redirects to
   `${FRONTEND_URL}/auth/callback?token=...`.
6. `AuthCallbackPage` reads the token from the query string, stores it in
   `localStorage`, and calls `GET /api/users/me` to populate `AuthContext`.
7. Every subsequent API call attaches `Authorization: Bearer <token>` via an
   axios request interceptor (`frontend/src/api/client.ts`); a response
   interceptor clears the token and redirects to `/login` on any 401.
8. `requireAuth` middleware verifies the JWT and attaches `req.userId` on the
   backend — there is no server-side session store.

There is also a **dev-only bypass**: `POST /api/auth/dev-login` upserts a
fixed `dev@momentum.local` user and returns a token directly, with no Google
round-trip. It's compiled out whenever `NODE_ENV=production`
(`backend/src/routes/auth.routes.ts`) — it exists so the app is fully
explorable before a developer has set up Google Cloud OAuth credentials.

**Mobile auth is a separate exchange, not a repeat of the browser flow.** The
Android app can't do a redirect-based OAuth dance the way a browser can, so it
does native Google Sign-In via Credential Manager to get a Google ID token,
then `POST`s it to `/api/auth/google/mobile`, which verifies it server-side
(`google-auth-library`'s `OAuth2Client.verifyIdToken`, audience = the same web
`GOOGLE_CLIENT_ID`) and returns the same JWT shape as the web callback — see
[api-reference.md](./api-reference.md). Both flows call the same
`upsertUserFromGoogleIdentity()`, so there's one user-upsert implementation
behind two different entry points.

## Native Android app

`android/` — a standalone Kotlin/Gradle project, sibling to `backend/` and
`frontend/`, **not** part of the root `package.json` workspaces and untouched
by `npm install`/Docker Compose. Opened separately in Android Studio.

**Why it exists at all:** the product spec's Google Fit integration is a dead
end (see the top of this doc and [roadmap.md](./roadmap.md)) — its intended
replacement, Android Health Connect, is on-device-only with no cloud API a
backend can call. Getting that data into Momentum genuinely requires a native
app running on the device, not a web feature.

```
android/
├── app/build.gradle.kts          # minSdk 26 (Health Connect client library floor)
└── app/src/main/kotlin/com/momentum/android/
    ├── MainActivity.kt            # single Activity, Compose content, branches on auth state
    ├── auth/
    │   ├── GoogleSignInManager.kt  # Credential Manager -> Google ID token
    │   ├── AuthRepository.kt       # exchanges ID token for a Momentum JWT, stores it
    │   ├── AuthViewModel.kt        # UI state, ViewModelProvider.Factory (constructs its own deps)
    │   └── TokenStore.kt           # EncryptedSharedPreferences, not plain SharedPreferences
    ├── network/
    │   ├── ApiClient.kt            # Retrofit + OkHttp + kotlinx-serialization, Bearer interceptor
    │   └── MomentumApi.kt          # Retrofit interface + @Serializable request/response models
    └── ui/                         # Compose + Material3: LoginScreen, SyncScreen, theme/
```

This sprint (auth scaffold) is deliberately narrow: sign in, store the token,
show the signed-in user's name via `GET /users/me`. No Health Connect
integration yet — that's the next two sprints (permission flow + manual sync,
then `WorkManager` periodic incremental sync).

**Configuration** mirrors the backend/frontend `.env` pattern:
`android/local.properties` (gitignored) holds `GOOGLE_WEB_CLIENT_ID` (the same
value as `backend/.env`'s `GOOGLE_CLIENT_ID` — Credential Manager's
`setServerClientId` must reference the **web** client for the backend's
`verifyIdToken` audience check to pass) and `API_BASE_URL`, documented in
`android/local.properties.example`. Getting Credential Manager's Google
Sign-In working also requires registering a **separate Android-type** OAuth
client in the same Google Cloud project (keyed by the app's SHA-1 signing
fingerprint) alongside the existing web client — verify the exact console
click-path at setup time; this project doesn't attempt to script that part.

**Networking base URL** depends on how the app reaches the Docker-hosted
backend: the standard Android emulator uses `10.0.2.2` (its alias for the
host's `localhost`) — the default in `build.gradle.kts`; a real device via USB
should instead run `adb reverse tcp:4000 tcp:4000` and use `localhost:4000`;
a real device over Wi-Fi needs the host's actual LAN IP plus a Windows
Firewall inbound rule for port 4000 (Docker already publishes the port, so the
firewall is the more likely blocker on this host).

**A note on verification:** this Kotlin/Gradle project was written carefully
by hand, following current Android/Compose/Credential-Manager API shapes, but
has not been compiled or run — there's no Android SDK, Gradle, or emulator in
the environment this was built in. Opening it in Android Studio (which can
generate the Gradle wrapper on first open) is the first real build/verify
step, not this document.

## Containerization

`docker-compose.yml` runs three sibling services on one Docker network:
`postgres`, `backend`, `frontend`. Backend and frontend bind-mount their
source directories for hot reload rather than baking source into the image,
so `docker compose build` is only needed after a dependency change, not every
edit.

Two non-obvious fixes baked into the setup:

- **Postgres runs on host port 5434, not 5432.** A native Postgres service
  already listening on 5432 on the host machine would otherwise silently
  intercept connections meant for the container (Prisma would connect
  successfully but to the wrong database, then fail auth). Inside the Docker
  network the backend still reaches Postgres on the standard internal
  port 5432 via the service name (`postgres:5432`) — only the host-side
  mapping changed.
- **The backend uses `nodemon` with `legacyWatch` (polling), not `tsx watch`'s
  native mode.** `tsx watch` relies on Node's `fs.watch`, which does not
  reliably receive change events for files inside a Docker bind mount on this
  platform — edits were silently not triggering a restart. Polling is less
  elegant but actually works across the bind mount. The frontend doesn't need
  this fix because Vite's own dev server watcher is configured with
  `usePolling: true` directly in `vite.config.ts`.

`backend/Dockerfile` also explicitly installs `openssl` on the `node:24-alpine`
base — without it, Prisma's query engine can't detect the right libssl version
on Alpine and every query fails at runtime with an opaque schema-engine error,
even though `prisma generate` completes without complaint at build time.

## Integrations

Of the three integrations in the original product spec, only WHOOP is a real,
buildable OAuth integration today — Google Fit's REST API has had developer
sign-ups frozen since May 2024 and sunsets entirely at the end of 2026, and
MyFitnessPal's API is closed to new developer applications. See
[data-model.md](./data-model.md#integrationconnection) for the schema and
[calculations.md](./calculations.md#whoop-workouts) for the data-mapping
formulas; this section covers the integration architecture itself.

**WHOOP OAuth diverges from the Google flow in one deliberate way.**
`GET /api/auth/google` is a plain `<a href>` full-page navigation because it
doesn't need to know who's asking. `GET /api/integrations/whoop/connect`
*does* need to know the already-logged-in Momentum user — but a full-page
browser navigation can't carry the `Authorization: Bearer` header the axios
interceptor normally attaches. So the frontend calls `/connect` as a normal
authenticated axios request first (returning `{ authorizeUrl }`), and only
then does `window.location.href = authorizeUrl` for the actual WHOOP
redirect.

**The OAuth `state` parameter is a self-contained signed token
(`signOAuthState`/`verifyOAuthState` in `backend/src/lib/jwt.ts`), not
server-side session storage.** The backend's `nodemon` setup restarts on
every source file save in dev (see Containerization above) — an in-memory
`Map<state, userId>` would silently break any WHOOP consent flow left
mid-transit while editing backend code. A signed, ~10-minute-expiry token
needs no storage and survives restarts.

**OAuth tokens are encrypted at rest** (`backend/src/lib/crypto.ts`,
AES-256-GCM, `ENCRYPTION_KEY` env var) — see
[data-model.md](./data-model.md#integrationconnection).

**Sync is pull-based only — on-demand "Sync now" plus (for the future Android
app) a periodic background poll — not webhooks.** WHOOP supports webhooks for
real-time push, but this project has no publicly reachable URL from local
Docker dev, so there's nowhere for WHOOP to push to. Flagged in
[roadmap.md](./roadmap.md) as a future enhancement if this is ever deployed
somewhere reachable.

## Design system / theming

Colors are CSS custom properties defined once in `frontend/src/styles/index.css`
(`--surface-1`, `--text-primary`, `--series-1..8`, `--status-good` etc.) and
consumed everywhere — Tailwind utility classes (`tailwind.config.ts` maps
`bg-surface`, `text-ink-secondary`, `bg-series-1`, ...) and chart components
alike (Recharts elements read the raw `var(--series-1)` string directly, since
SVG fill/stroke props don't go through Tailwind).

Light is the default; dark is a **selected** theme, not an automatic filter —
`@media (prefers-color-scheme: dark)` supplies the OS-level default, and a
`data-theme="dark"|"light"` attribute on `<html>` (toggled by the user in
Settings → Appearance) overrides it in both directions. The preference is
applied in `main.tsx` before the app renders (reading `localStorage` via
`frontend/src/lib/theme.ts`) so there's no flash of the wrong theme on load;
choosing "System" clears the stored preference and falls back to the media
query.

Colors follow fixed roles, never assigned ad hoc:
- **Categorical (pillar identity):** Training = blue (`--series-1`), Fuel =
  orange (`--series-2`), Recovery = aqua (`--series-3`), Body = yellow
  (`--series-4`), Goals = magenta (`--series-5`) — used consistently for chart
  lines, timeline entry dots, and goal-type accents.
- **Status (state, never identity):** good/warning/serious/critical
  (`--status-*`) are reserved for readiness levels and goal status, and always
  paired with a text label or icon — never color alone, since several of
  these steps are sub-3:1 contrast by design on the light surface.
