# API Reference

Base path: `http://localhost:4000/api`. Every route except `/auth/*` requires
`Authorization: Bearer <jwt>` (see [architecture.md](./architecture.md#auth-flow)).
Request/response shapes are the DTOs in `packages/shared/src/dto.ts` — this
page lists routes and intent, not full field-level schemas.

## Auth — `auth.routes.ts`

| Method | Path | Notes |
|---|---|---|
| GET | `/auth/google` | Starts the OAuth flow. 503 if `GOOGLE_CLIENT_ID/SECRET` aren't set. |
| GET | `/auth/google/callback` | OAuth callback; redirects to frontend with a JWT. |
| POST | `/auth/dev-login` | Dev-only bypass — issues a token for a fixed local user. Disabled when `NODE_ENV=production`. |
| GET | `/auth/status` | `{ googleOAuthConfigured: boolean }` — lets the login page hide the Google button if unconfigured. |
| POST | `/auth/logout` | Stateless no-op (204) — the frontend just discards its token. |

## Users — `user.routes.ts`

| Method | Path | Notes |
|---|---|---|
| GET | `/users/me` | Current profile. |
| PATCH | `/users/me` | Update name/unitSystem/dateOfBirth/heightCm/estimatedDailyBurnKcal. |
| POST | `/users/me/onboarding` | Completes onboarding; optionally creates an initial `WeightEntry` and one or two `Goal` rows. |
| POST | `/users/me/onboarding/skip` | Marks onboarding skipped. |
| GET | `/users/me/export` | Full JSON dump of every owned row (data portability). |
| DELETE | `/users/me` | Cascades to all owned data. |

## Weight (Body) — `weight.routes.ts`

| Method | Path | Notes |
|---|---|---|
| GET | `/weight?from&to` | Raw entries, most recent first. |
| GET | `/weight/trend?from&to` | Raw + moving average + rate of change + goal weight — see [calculations.md](./calculations.md#weight-trend). |
| POST | `/weight` | Create an entry. Triggers streak-milestone check. |
| PATCH | `/weight/:id` | Update; sets `isManuallyEdited: true`. |
| DELETE | `/weight/:id` | |

## Nutrition (Fuel) — `nutrition.routes.ts`

| Method | Path | Notes |
|---|---|---|
| GET | `/nutrition?from&to` | Raw meal entries. |
| GET | `/nutrition/summary?date` | Daily totals + target comparison + estimated energy balance. |
| GET | `/nutrition/summary/range?from&to` | Summary per day across a range (for weekly charts). |
| GET | `/nutrition/energy-balance?granularity=day\|week\|month\|year` | Consumed-vs-burned series bucketed by the requested granularity (default `day`) — see [calculations.md](./calculations.md#consumed-vs-burned-series-dayweekmonthyear). Default lookback: 30 days / 12 weeks / 12 months / 5 years. |
| POST | `/nutrition` | Create a meal entry. Triggers streak-milestone check. |
| PATCH | `/nutrition/:id` | |
| DELETE | `/nutrition/:id` | |

## Training — `training.routes.ts`

| Method | Path | Notes |
|---|---|---|
| GET | `/training?from&to&type` | List sessions (expands `workout.exercises.sets` / `matchDetail`). |
| GET | `/training/:id` | Single session, full detail. |
| POST | `/training` | Create a session. Body shape depends on `type` (see `training.validation.ts`'s `superRefine`: `GYM` requires `workout.exercises`, `MATCH` requires `matchDetail`, no other type may include either). Runs PR detection inline; triggers streak-milestone check. |
| PUT | `/training/:id` | Full replace — deletes and recreates the `workout`/`matchDetail` subtree in one transaction rather than diffing. |
| DELETE | `/training/:id` | |
| GET | `/training/gym/exercises` | Distinct exercise names logged by this user (autocomplete / goal picker). |
| GET | `/training/gym/exercises/:name/progression` | Per-session best weight, estimated 1RM, volume, and PR flag for one exercise. |
| GET | `/training/load-trend?from&to` | Daily summed training load over a range. |
| GET | `/training/load-summary` | Weekly load, sessions this week, 7d:28d ACWR. |

Note: the two `gym/exercises*` routes are registered before `/:id` in the
router so Express doesn't try to match `gym` as a session ID.

## Recovery — `recovery.routes.ts`

| Method | Path | Notes |
|---|---|---|
| GET | `/recovery/today` | Today's record (with computed score/level/recommendation), or `null`. |
| GET | `/recovery?from&to` | History. |
| POST | `/recovery` | Upserts by `(userId, date)` — logging again the same day updates rather than duplicates. Recomputes readiness score against the user's rolling baseline. Triggers streak-milestone check. |
| DELETE | `/recovery/:id` | |

## Goals — `goal.routes.ts`

| Method | Path | Notes |
|---|---|---|
| GET | `/goals?status&type` | Every goal's status/progress is recomputed live before returning (see [calculations.md](./calculations.md#goal-progress--status)). |
| GET | `/goals/:id` | |
| POST | `/goals` | Snapshots the current metric value as `startValue` at creation time. |
| PATCH | `/goals/:id` | Update title/target/targetDate/notes, or pause/resume via `status`. |
| DELETE | `/goals/:id` | |

## Insights — `insights.routes.ts`

| Method | Path | Notes |
|---|---|---|
| GET | `/insights` | All insights that currently clear their materiality threshold — see [calculations.md](./calculations.md#insights). Computed live, not cached. |

## Timeline — `timeline.routes.ts`

| Method | Path | Notes |
|---|---|---|
| GET | `/timeline?from&to` | Merges weigh-ins, meals, training sessions, recovery records, and achievements into one feed, sorted newest-first. `from`/`to` are required (unlike most other range queries, which default when omitted). |

## Achievements — `achievement.routes.ts`

| Method | Path | Notes |
|---|---|---|
| GET | `/achievements?type` | All achievements for the user, most recent first. |

## Dashboard — `dashboard.routes.ts`

| Method | Path | Notes |
|---|---|---|
| GET | `/dashboard/today` | Composition endpoint for the Today page — readiness, weight/nutrition/training summaries, today's timeline, active goals (top 4, excluding achieved/paused), streaks + recent achievements, top 3 insights. Calls the exact same service functions the granular endpoints use. |

## Integrations — `integrations.routes.ts`, `whoop.routes.ts`, `healthConnect.routes.ts`

| Method | Path | Notes |
|---|---|---|
| GET | `/integrations` | Connection status for every known provider (WHOOP, Health Connect), even before any row exists for the user. |
| GET | `/integrations/whoop/connect` | Behind `requireAuth` (called via axios, not a plain link — see [architecture.md](./architecture.md#integrations)). Returns `{ authorizeUrl }`. 503 if `WHOOP_CLIENT_ID/SECRET` aren't set. |
| GET | `/integrations/whoop/callback` | Not behind `requireAuth` — WHOOP's redirect carries no Bearer header. Auth comes from verifying the signed `state` param. Redirects to `/settings?whoop=connected` or `?whoop=error&message=...`. |
| POST | `/integrations/whoop/sync` | Fetches recovery/sleep/workout data since the last successful sync (or last 30 days on first sync) and upserts it — see [calculations.md](./calculations.md#whoop-workouts). Always returns 200 with a structured `WhoopSyncResultDto` (`status: "SUCCESS" \| "ERROR"`), even on failure, so the frontend can show a visible error state rather than a bare exception. |
| DELETE | `/integrations/whoop` | Disconnect — nulls token columns, does **not** delete previously-synced data. |
| POST | `/integrations/health-connect/sync` | Batch sync endpoint for the companion Android app (weight/exercise/sleep records) — lands in a later sprint alongside the app itself. |
