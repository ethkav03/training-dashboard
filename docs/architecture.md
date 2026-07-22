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
end (see [Integrations](#integrations) below and [roadmap.md](./roadmap.md))
— its intended replacement, Android Health Connect, is on-device-only with no
cloud API a backend can call. Getting that data into Momentum genuinely
requires a native app running on the device, not a web feature.

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
    │   ├── MomentumApi.kt          # Retrofit interface -- full REST surface, one section per backend route file
    │   └── dto/                    # @Serializable data classes hand-ported from packages/shared/src/{dto.ts,enums.ts}
    ├── data/                       # one Repository per resource (WeightRepository, GoalRepository, ...) -- thin suspend wrappers over MomentumApi, mirrors frontend/src/api/*.ts
    ├── healthconnect/
    │   ├── HealthConnectManager.kt     # SDK availability check, fixed permission set, permission contract
    │   ├── HealthConnectRepository.kt  # bounded + changes-token reads (weight/exercise/sleep) + per-session aggregates
    │   ├── HealthConnectMapper.kt      # SDK records -> the exact JSON shape the sync endpoint expects
    │   ├── HealthConnectSyncState.kt   # DataStore-persisted changes-token, on-device only
    │   └── HealthConnectViewModel.kt   # UI state, ViewModelProvider.Factory (constructs its own deps)
    ├── sync/
    │   ├── SyncWorker.kt               # CoroutineWorker: same read-map-post pipeline, changes-token driven
    │   └── SyncScheduler.kt            # enqueues SyncWorker as ~6h unique periodic WorkManager work
    └── ui/
        ├── LoginScreen.kt          # Compose + Material3
        ├── navigation/             # MomentumDestination (6 tabs, mirrors web's NAV_ITEMS), MomentumBottomBar, MomentumNavHost
        ├── screens/                # TodayScreen, ProgressScreen (Body/Fuel/Recovery sub-tabs), TrainingScreen + ExerciseProgressionScreen, GoalsScreen, SettingsScreen (real) + a placeholder for Insights
        ├── forms/                  # WeightEntryForm, NutritionEntryForm, TrainingSessionForm, RecoveryEntryForm, GoalForm -- mirrors frontend/src/components/forms/, one file per entity, same component for create+edit
        ├── charts/                 # MomentumChartCard (chart/table toggle) + WeightTrendChart/EnergyBalanceChart/ExerciseProgressionChart (Vico) -- mirrors frontend/src/components/charts/
        ├── components/             # MomentumCard, MomentumButton, MomentumModalSheet -- mirrors frontend/src/components/ui/
        ├── cards/                  # ReadinessBadge, GoalCard, InsightCard -- mirrors frontend/src/components/cards/
        ├── timeline/               # TimelineEntryItem -- shared by Today's preview and the full Timeline screen (Sprint 20)
        └── theme/                  # Color/Theme/Type -- palette ported from frontend/src/styles/index.css
```

Auth (Sprint 11) stayed deliberately narrow: sign in, store the token, show
the signed-in user's name via `GET /users/me`. Sprint 12 added Health Connect:
permission request + a manual "Sync now" that reads a bounded history and
posts it to `POST /integrations/health-connect/sync`. Sprint 13 added the
background half — periodic `WorkManager` sync using Health Connect's
changes-token so it isn't re-reading 30 days of history every ~6 hours.

**Sprint 15 begins full feature parity with web** (see roadmap.md) — a real
bottom-nav shell (`ui/navigation/`) with the same six destinations as web's
`NAV_ITEMS`, a full DTO/Retrofit/repository layer covering every backend
route, and a ported color palette (`ui/theme/Color.kt`, matching
`frontend/src/styles/index.css`'s categorical/status colors exactly, light
and dark). The old standalone `SyncScreen` is gone — its Health Connect
permission/sync UI moved into the new `SettingsScreen`, alongside profile
info, matching where that same functionality lives in web's Settings page.
Today/Progress/Training/Goals/Insights are placeholder screens until their
own sprints (16-20) build the real thing.

**Sprint 16 builds the Today screen** (`ui/screens/TodayScreen.kt` +
`TodayViewModel.kt`), mirroring `TodayPage.tsx`: readiness card, four stat
tiles, active-goals strip, top insights, recent achievements, today's
timeline (`ui/timeline/TimelineEntryItem.kt`, shared with the future full
Timeline screen), and quick-action buttons. Those buttons navigate to the
relevant tab rather than opening an inline log form for now — the entry
forms themselves don't exist yet (Sprints 17-19 build Body/Fuel/Training/
Recovery/Goals) — an honest interim behavior, not the intended final one.

This sprint also changed how tab switches behave, app-wide.
`MomentumBottomBar` used to pass `saveState`/`restoreState = true` to
`navController.navigate()` — the standard Navigation-Compose idiom for
preserving each tab's scroll position and ViewModel across switches. But web
deliberately does the opposite for its dashboard/summary queries
(`refetchOnMount: "always"`), specifically so a write made in one tab (log a
meal in Fuel) shows up immediately back on Today rather than showing stale
cached data. Matching that behavior mattered more than preserving scroll
position, so `saveState`/`restoreState` were dropped: every tab switch now
fully recomposes the destination, and each screen's own
`LaunchedEffect(Unit) { viewModel.refresh() }` naturally re-fires on every
visit. This applies to every future screen the same way, not just Today.

**Sprint 17 builds Body and Fuel** (`ui/screens/{Body,Fuel}Screen.kt` +
ViewModels), the first two tabs under a new `ProgressScreen` that hosts
Body/Fuel/Recovery as sub-tabs via Material3 `TabRow`, mirroring how web
nests them under one "Progress" nav item rather than three separate ones.
Recovery is listed in the tab strip now (as a "coming later" placeholder)
specifically so the tab positions don't reshuffle once Sprint 19 fills it
in. Both screens follow the same shape as Today: a ViewModel wrapping the
relevant repository, stat tiles, a chart, an entry list with edit/delete,
and a `ModalBottomSheet` form (`ui/forms/WeightEntryForm.kt` /
`NutritionEntryForm.kt`) shared between create and edit.

New `ui/charts/` package: `MomentumChartCard` ports `ChartCard.tsx`'s
chart/table toggle and conditional legend (only shown for 2+ series) without
depending on Vico at all -- it just takes `chart`/`tableContent` Composable
slots, so the toggle works identically regardless of what's inside.
`WeightTrendChart`/`EnergyBalanceChart` are the first real use of Vico
(picked in the Android-parity planning session over hand-rolled Canvas
charts or the View-based MPAndroidChart). **Two deliberate scope trims,
both because Vico's exact API for combining multiple mark types on one
chart is the part of it I'm least confident about verbatim, not because the
data doesn't support it:** `WeightTrendChart` renders only the smoothed
7-day average line, not the raw-weigh-in scatter or the dashed goal-weight
reference line web also shows; `EnergyBalanceChart`'s Vico setup (two-series
grouped columns) is similarly best-recollection rather than confirmed
against Vico's actual docs. Both are flagged inline with a link to
Vico's guide to check against if they don't compile as written.

**One real bug from this stretch worth remembering: Kotlin block comments
nest.** Unlike C/Java/TS, `/* ... */` can nest in Kotlin, so a KDoc comment
that happens to contain the literal text `/*` anywhere in its prose (e.g. a
glob pattern like `routes/*.routes.ts` typed straight into a comment)
silently opens an unterminated nested comment that swallows everything after
it to end of file. This actually happened to `MomentumApi.kt`'s interface
doc comment and took the entire interface down with it -- every repository
and ViewModel that calls `MomentumApi` showed "unresolved reference" even
though the bug was in one file. Worth a second look at any future doc
comment that mentions a file-path glob.

**Sprint 18 builds Training and exercise progression**
(`ui/screens/TrainingScreen.kt` + `ExerciseProgressionScreen.kt`), mirroring
`TrainingPage.tsx` and `training/ExerciseProgressionPage.tsx`: load-summary
tiles (with an ACWR-over-1.5 warning), exercise-name chips that navigate to
a nested `training/exercises/{exerciseName}` route (the first non-tab
destination in the nav graph -- exercise names are URI-encoded via
`Uri.encode`/`Uri.decode` since they can contain spaces), and a recent-
sessions list expanding into per-exercise set summaries or match details.

`ui/forms/TrainingSessionForm.kt` is the largest form in the app --
activity-type-conditional sections (gym exercises-with-sets, match detail +
free-form key-stats), matching `TrainingSessionForm.tsx`'s shape exactly.
Compose has no `useFieldArray` equivalent, so the dynamic exercise/set/
key-stat lists use a small state-holder-class pattern instead: each
`ExerciseFormState`/`SetFormState`/`KeyStatFormState` holds its own
`mutableStateOf` fields inside a `SnapshotStateList`, so editing one input
only recomposes that row rather than the whole list. Same "no date picker
yet" trim already accepted for `WeightEntryForm` -- defaults to now (or the
session's existing date for edits).

Written with if/else throughout instead of early returns this time, per the
lesson from Sprint 17: neither `MomentumCard` nor `MomentumButton` is
`inline`, so a labeled non-local return from their content/onClick lambdas
is never legal Kotlin.

**A `LazyColumn` inside `verticalScroll` crashed on launch.** Right after
sign-in, `TodayScreen`'s timeline preview used a `LazyColumn` nested inside
the screen's own top-level `Column(Modifier.verticalScroll(...))` --
`LazyColumn` needs bounded height to know how much to lazily compose, and an
unbounded scrollable parent hands it infinite height instead, which Compose
treats as a hard `IllegalStateException`, not a silent bug. Today's timeline
is small and already fully loaded (nothing virtualization would help with
anyway), so it's a plain `Column` + `forEach` now, matching every other list
on that screen. Worth checking for the same shape (`LazyColumn`/`LazyRow`
directly inside a `verticalScroll`/`horizontalScroll` container) before
adding either to a new screen.

**WHOOP manual sync from Android.** The original Android-parity plan kept
WHOOP fully read-only on mobile to avoid building Custom-Tabs OAuth capture
for connect/disconnect. But *syncing an already-connected account* needs no
OAuth at all -- it's the same authenticated `POST /integrations/whoop/sync`
the web "Sync now" button already calls, and the backend doesn't care which
client sends it. `SettingsScreen` now has a WHOOP card (status, last-synced
time, a "Sync now" button when connected) backed by a new
`IntegrationsViewModel` -- connect/disconnect still only exist on web.

**Dark/Light/System appearance**, matching web's own three-way toggle.
`ThemePreferenceStore` persists the choice via DataStore (same pattern as
`HealthConnectSyncState`), `ThemeViewModel` lives at the Activity level
alongside `AuthViewModel`/`HealthConnectViewModel` so `MainActivity` can
resolve the actual boolean `MomentumTheme(darkTheme = ...)` needs before
gating on auth state, and `SettingsScreen` exposes the same choice web's
Appearance section does. `MomentumTheme`'s `darkTheme` parameter already
defaulted to `isSystemInDarkTheme()`; `MainActivity` now always passes an
explicit resolved value instead of relying on that default.

**Icon buttons over text**, per user request. New
`ui/components/MomentumEditDeleteActions.kt`: `MomentumEditDeleteActions`
(paired pen/trash `IconButton`s, for every entry list's Edit/Remove pair --
Body, Fuel, Training, `GoalCard`) and `MomentumDeleteIconButton` (a lone
trash icon for form-internal "remove this row" actions with no paired edit,
e.g. `TrainingSessionForm`'s exercise/set/key-stat rows).
`MomentumModalSheet`'s literal `"✕"` close button became a proper close
icon too. Needed one new dependency,
`androidx.compose.material:material-icons-core` (the small curated core
icon set -- Edit/Delete/Close and similar -- not
`material-icons-extended`'s ~2000 icons, which would've been overkill for a
handful of common glyphs); its version comes from the existing Compose BOM,
not guessed independently the way Vico's was.

**Sprint 19 builds Recovery and Goals**, filling in `ProgressScreen`'s last
sub-tab and the app's last data-entry tab, mirroring
`progress/RecoveryTab.tsx` and `GoalsPage.tsx`:

- `RecoveryScreen`/`RecoveryEntryForm`: today's readiness (score/badge/
  sleep/yesterday's strain/recommendation) plus history, matching web's
  three-stat layout already used on Today. `RecoveryEntryForm` is upsert-
  only, same as web -- there's no separate edit mode, since logging again
  for today just updates that day's row (the backend keys on `(userId,
  date)`).
- `GoalsScreen`/`GoalForm`: goal-type-conditional exercise picker (only for
  `EXERCISE_PERFORMANCE`), pulling exercise names via a small standalone
  `TrainingRepository` fetch inside the form rather than threading them down
  from the screen -- a self-contained concern, same as web's own
  `useExerciseNames()` call living inside `GoalForm.tsx` rather than
  `GoalsPage.tsx`. `GoalCard`'s `onTogglePause`/`onDelete` callbacks (written
  back in Sprint 15 as presentation-only props, unused until now) finally
  get real implementations wired to `GoalsViewModel`.

**Auto-sync on login.** `HealthConnectViewModel`'s `init` block (which only
ever runs once per sign-in — this ViewModel is first referenced from
`MomentumNavHost`, itself only shown once `authState.token != null`) checks
permission status and, if Health Connect access is already granted, calls
`syncNow()` immediately rather than waiting for a manual tap. This mirrors
the backend auto-syncing WHOOP on every login (see
[api-reference.md](./api-reference.md#auth--authroutests)) — the goal is
that whichever provider a user has connected, opening either app pulls fresh
data without them having to remember to. `refreshStatus()` (called every
time the screen is revisited) deliberately does **not** re-trigger a sync —
only the one-time `init` path does — so navigating back to this screen
doesn't spam syncs on its own.

**Health Connect read path**, `healthconnect/`:

- `HealthConnectManager` checks `HealthConnectClient.getSdkStatus()` first —
  the provider app can be missing or out of date on pre-Android-14 devices —
  before ever touching `HealthConnectClient.getOrCreate()`, which throws if
  it's not available. The client itself is built lazily so constructing the
  manager (which happens as soon as the screen loads) can never crash on an
  unsupported device; only an actual sync attempt can.
- Permission set is fixed: steps, exercise, weight, heart rate, sleep (all
  read-only). Steps is requested — completing the permission flow the
  product spec describes — but isn't written anywhere; there's no column for
  it in the current schema, so it's a roadmap item, not a bug.
- The manual **"Sync now"** button always reads a **fixed 30-day trailing
  window** — simple and always correct, since a user tapping a button once
  in a while doesn't need incremental efficiency. The **background** job
  (below) uses Health Connect's changes-token instead. Either way, re-sending
  already-synced records is safe: the backend's `externalId`-based dedup (see
  [data-model.md](./data-model.md)) makes it a no-op.
- Exercise sessions carry no energy/heart-rate totals on the record itself —
  Health Connect stores those as separate time-series records — so each
  session gets its own `aggregate()` query scoped to that session's own
  `[startTime, endTime]`. Calories use `ActiveCaloriesBurnedRecord`, not
  `TotalCaloriesBurnedRecord` — the latter folds in resting metabolic rate for
  the whole window, which would overcount "calories burned by this workout."
- Sleep minutes are computed by summing only the stages actually asleep
  (excluding awake-in-bed / out-of-bed stage types), falling back to the
  full session span when a device reports no stage detail at all — see
  [calculations.md](./calculations.md#readiness-score) for what happens to
  that number once it reaches the backend (it runs through
  `computeReadiness()`, same as any Health-Connect-sourced day).
- `HealthConnectMapper`'s exercise-type lookup table is a hand-kept mirror of
  `backend/src/services/healthConnectService.ts`'s
  `EXERCISE_TYPE_TO_ACTIVITY_TYPE` — the two must be changed together. An
  exercise type missing from both sides isn't a sync failure; it just maps to
  `OTHER` server-side.
- The manifest additions this needs: five `android.permission.health.READ_*`
  permissions, a `<queries>` entry for Health Connect's package (so
  `getSdkStatus()` can actually detect it pre-Android-14), and a
  `ViewPermissionUsageActivity` alias — required by Health Connect's own
  permission-rationale screen, not a normal launchable activity.

**Background sync**, `sync/` + `healthconnect/HealthConnectSyncState.kt`:

- `SyncScheduler.schedule()` enqueues `SyncWorker` as **unique periodic work**
  (`ExistingPeriodicWorkPolicy.KEEP`, ~6h interval, network-required
  constraint), called from `HealthConnectViewModel` every time it confirms
  Health Connect permissions are actually granted — safe to call repeatedly
  since `KEEP` means a second call never resets an already-scheduled job.
- `SyncWorker` (`CoroutineWorker`) is built by WorkManager's default
  reflective factory, not any custom DI — every dependency it needs
  (`TokenStore`, `HealthConnectManager`, `HealthConnectRepository`, the
  Retrofit `ApiClient`) is constructed by hand inside `doWork()`, the same
  pattern every `ViewModel.Factory` in this app already uses. It exits
  cleanly (`Result.success()`, not a failure) when there's no signed-in user,
  Health Connect isn't available, or permissions aren't granted — none of
  those are error conditions for a background job that may run long after
  the user last opened the app.
- **Changes-token, not a bounded re-read:** `HealthConnectSyncState`
  persists Health Connect's own incremental changes-token via Jetpack
  DataStore, scoped to exactly the three record types this app actually
  syncs (weight, exercise, sleep — tracking heart rate or steps here would
  just burn the token's change-log budget for data we only ever read as an
  aggregate or don't sync at all). `HealthConnectRepository.readChanges()`
  pages through `getChanges()` until `hasMore` is false, then returns the
  accumulated upserts plus the token to persist for next time.
- **Token expiry falls back to the same bounded 30-day read** "Sync now"
  uses, immediately followed by minting and storing a brand-new token —
  Health Connect only guarantees a changes-token stays valid for a limited
  window, so a device that hasn't run its background sync in a while (long
  idle period, app data cleared) needs a bounded do-over rather than a
  permanent failure.
- **Deletions aren't propagated.** `readChanges()` only acts on
  `UpsertionChange` — an on-device delete doesn't retract an already-synced
  Momentum row. This is the same accepted row-level coarseness already
  documented for manual-edit precedence (see
  [data-model.md](./data-model.md)), not an oversight.
- **"Roughly" 6 hours, honestly.** WorkManager periodic work is subject to
  Doze/App Standby — the OS can and will delay it well past the requested
  interval if the device is idle or in a restricted standby bucket. This
  build doesn't fight that with a foreground service, exact alarms, or a
  battery-exemption prompt (see [roadmap.md](./roadmap.md)'s Android scope
  cuts) — background sync is explicitly best-effort, with the manual "Sync
  now" button as the reliable path whenever the user actually wants fresh
  data immediately.

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

**Router mount order matters here, and got it wrong once.** `app.ts` mounts
`/api/integrations`, `/api/integrations/whoop`, and
`/api/integrations/health-connect` as three separate routers. Express matches
`app.use()` mounts by path *prefix*, in registration order — `/api/integrations`
is itself a prefix of the other two paths, so if it's registered first, every
request under `/api/integrations/whoop/*` or `/api/integrations/health-connect/*`
gets routed into `integrationsRouter` first. That router's blanket
`integrationsRouter.use(requireAuth)` runs regardless of whether a route
inside it actually matches — so a request that reaches it with no
`Authorization` header fails there immediately and never falls through to
`whoopRouter`/`healthConnectRouter` at all. This went unnoticed for a while
because every *authenticated* request (`/connect`, `/sync`, `/disconnect`,
all called via axios with a Bearer token already attached) still succeeds:
`requireAuth` passes, no route matches inside `integrationsRouter`, and
Express falls through to the next mount. WHOOP's own OAuth **callback**
redirect is the one request in this whole flow that deliberately carries no
Bearer token (see below) — so it was the one path that actually surfaced the
bug, as a raw `{"error":"Unauthorized"}` instead of completing the connection.
Fixed by mounting the two more specific routers before the general one — the
rule going forward: **more specific `app.use()` paths must always be
registered before less specific ones that share a prefix.**

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
