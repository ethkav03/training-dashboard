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
4. The Passport verify callback does `prisma.user.upsert({ where: { googleId } })`
   — first login creates the user, subsequent logins just refresh
   name/email/avatar.
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
