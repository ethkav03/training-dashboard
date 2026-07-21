# Development Roadmap

Tracks the sprint plan agreed for this build, what's done, and what's
explicitly out of scope. Update the status column as work lands — this file
is the source of truth for "what's built" at a glance; see
[api-reference.md](./api-reference.md) and [calculations.md](./calculations.md)
for the implementation detail behind each item.

## Sprint status

| Sprint | Scope | Status |
|---|---|---|
| 0 | Monorepo scaffold: npm workspaces, docker-compose, Prisma `User` model, Express health check, Vite + Tailwind boot | Done |
| 1 | Auth end-to-end (Google OAuth + JWT + dev-login bypass), onboarding flow, responsive nav shell, Settings (profile/units/export/delete) | Done |
| — | **Containerization** (not originally its own sprint — added mid-build at user request): backend + frontend as sibling Docker Compose services alongside Postgres, hot-reload bind mounts | Done |
| 2 | Body pillar (manual weight entry, 7-day moving average trend, rate of change, goal weight) + Fuel pillar (meal logging, daily calorie/macro summary, estimated energy balance) | Done |
| 3 | Training system: all activity types, Gym workouts (nested exercises/sets, training load, estimated 1RM, PR detection groundwork), Match/sport tracking (opponent, result, rating, key stats) | Done |
| 4 | Recovery pillar (manual sleep/soreness/energy entry → weighted readiness score) + Goals system (type-aware progress tracking, status computation) | Done |
| 5 | Today dashboard composition endpoint + full TodayPage (hero readiness, metric row, goals strip, timeline, quick actions), unified Timeline page (week-at-a-time cross-pillar feed) | Done |
| 6 | Insights engine (6 deterministic trend rules) + Gamification (Achievement persistence: PR detection on gym sessions, streak milestones at 7/30/100 days) | Done |
| — | `/docs` folder (architecture, data model, calculations, API reference) — added mid-build at user request, kept updated per-change going forward | Done |
| 7 | Polish: responsive/accessibility pass on charts (legends, table views), dark-mode toggle wiring, empty states, root README with setup instructions | Done |
| — | **Interim fixes** (mid-build, not their own sprint): edit support for weight/meal/training-session entries (previously create+delete only); Today-tab staleness fix (`refetchOnMount: "always"` on dashboard/summary queries); `tsconfig.node.json` emitting a stray `vite.config.js` on every typecheck | Done |
| 8 | Shared integration infrastructure: `IntegrationConnection` model (encrypted-at-rest OAuth tokens), `externalId` dedup + manual-edit precedence (`externalSyncService.ts`), `HEALTH_CONNECT` data source, `GET /api/integrations` | Done |
| 9 | WHOOP OAuth connect/callback/disconnect (real endpoints, scopes verified against developer.whoop.com), signed-state CSRF protection, Settings UI (connect/sync-now/disconnect, visible error state) | Done |
| 10 | WHOOP data sync: real recovery/sleep/workout fetch (paginated), field mapping (sport→ActivityType, strain→intensity, kJ→kcal), sync result summary in Settings | Done |
| 10.5 | Fuel: calories-consumed-vs-burned indicator + day/week/month/year energy-balance graph | Done |
| 11 | Mobile auth (`POST /auth/google/mobile`, shared `upsertUserFromGoogleIdentity`) + native Android app scaffold (Kotlin, Google Sign-In only, no Health Connect yet) | Done (backend verified via curl; Android project written but not yet compiled/run -- no Android SDK/emulator in the build environment, see [architecture.md](./architecture.md#native-android-app)) |
| 12 | Health Connect read + manual sync from the Android app (bounded historical read, mapper, `POST /integrations/health-connect/sync`) | Done (backend verified via curl; Android client written — `HealthConnectManager`/`Repository`/`Mapper`/`ViewModel`, `SyncScreen` wiring — but not yet compiled/run, same caveat as Sprint 11, see [architecture.md](./architecture.md#native-android-app)) |
| — | **Interim feature additions** (mid-build, not their own sprint): meal-type categorization (`NutritionEntry.mealType`: Breakfast/Lunch/Dinner/Snacks, matching MyFitnessPal's diary layout, with time-of-day default); Recovery now logs `sleepScore` (0–100) and `strain` (0–21, 1 decimal) as pass-through headline metrics alongside the existing readiness score, with WHOOP sync computing "yesterday's strain" via previous-completed-cycle lookup | Done |
| 13 | Android `WorkManager` periodic incremental sync (Health Connect changes-token, token-expiry fallback) | Done (backend unaffected; Android client written — `SyncWorker`/`SyncScheduler`/`HealthConnectSyncState` — but not yet compiled/run, same caveat as Sprints 11–12, see [architecture.md](./architecture.md#native-android-app)). Web Settings' Health Connect row shipped earlier than planned, alongside Sprint 9's WHOOP row; on-device steps display deferred, see below. |
| 14 | Docs pass across all five `/docs` files for Sprints 8–13 + `.env.example` updates + Settings polish | Done |

## Explicitly out of scope for this build

Called out here so they don't get silently assumed as "missing bugs" later —
these were deliberate scope cuts, not gaps:

- **Google Fit's REST API.** Developer sign-ups have been frozen since May
  2024 and the whole API sunsets end of 2026 — not viable for new
  development. Google-side health data instead comes from Android Health
  Connect via a native companion app (Sprints 11–13).
- **MyFitnessPal.** Their API is closed to new developer applications; no
  integration was built. Meals are logged manually via Progress → Fuel.
- **WHOOP webhooks.** WHOOP supports real-time push, but this project has no
  publicly reachable URL from local Docker dev. Sync is pull-based (manual
  "Sync now" + a future periodic poll) — see
  [architecture.md](./architecture.md#integrations).
- **Push/email notifications** (goal reminders, sync-failure alerts, weekly
  summaries) — no notification infrastructure at all.
- **Product usage analytics/telemetry** (weekly active users, sync success
  rate, etc.) — not meaningful for a single-user personal build.
- **Advanced AI coaching / training plan generation** — Insights are
  deliberately simple, deterministic, and self-explaining rather than a
  black-box recommendation engine (see [calculations.md](./calculations.md#insights)).
- **Exercise library** — exercise names in Gym workouts are free text; no
  reference table of standard exercises with technique notes.
- **Weekly-completion / "perfect week" achievement** — the raw percentage is
  surfaced on the dashboard, but it isn't (yet) turned into a persisted,
  non-duplicating achievement (needs week-scoped dedup logic not built yet).
- **PR detection on session edits** — only fires when a gym session is
  first created, not when an existing session is edited, and never for
  WHOOP/Health-Connect-synced sessions (no per-set data to evaluate).
- **Multi-device-per-user, offline queueing, and battery-optimization UX**
  for the Android app — assume one device per account; background sync is
  best-effort only, not fought against Doze/App Standby.

## Possible next steps (not committed to)

Roughly in order of how directly they build on what exists:

1. Weekly/monthly calendar grid view for Timeline, beyond the current
   week-at-a-time list.
2. `WEEKLY_COMPLETION` achievement type with week-scoped dedup.
3. Exercise library table, referenced by ID instead of free-text name
   matching — would make `EXERCISE_PERFORMANCE` goals and PR detection exact
   rather than string-matched.
4. Field-level (not row-level) manual-edit tracking for synced records, so
   hand-editing one field of a WHOOP/Health-Connect day doesn't block that
   whole day's row from ever being re-synced.
5. WHOOP webhook support + `WEEKLY_COMPLETION`, if this is ever deployed
   somewhere with a public URL.
6. On-device steps display in the Android app. `StepsRecord` permission is
   already requested (completes the Health Connect permission flow) and read
   access is granted, but steps has no column anywhere in the current schema
   — this would show a local, phone-only steps figure without syncing it
   anywhere, or else needs a real schema decision (new table? folded into
   `RecoveryRecord`?) first.
