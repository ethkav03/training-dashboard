# Data Model

Full schema: `backend/prisma/schema.prisma`. This walks through the intent
behind each entity rather than repeating the field list verbatim.

## Cross-cutting conventions

- **Canonical units.** All physical quantities are stored in metric (kg, cm)
  regardless of the user's display preference (`User.unitSystem`) — unit
  conversion is a frontend display concern only, never a storage concern.
- **Source tracking.** Every loggable record (`WeightEntry`, `NutritionEntry`,
  `TrainingSession`, `RecoveryRecord`) carries a `source` enum
  (`MANUAL | GOOGLE_FIT | WHOOP | MYFITNESSPAL | HEALTH_CONNECT`) and an
  `isManuallyEdited` boolean. `GOOGLE_FIT` is vestigial — Google's Fit REST API
  is being sunset (developer sign-ups have been frozen since May 2024), so
  Google-sourced data instead arrives via `HEALTH_CONNECT` (the Android
  on-device platform, synced by a companion native app — see
  [architecture.md](./architecture.md#integrations)). `MYFITNESSPAL` exists in
  the enum but nothing writes it: MyFitnessPal's API is closed to new
  developers, so that integration was dropped rather than built.
- **Dedup for synced records.** `WeightEntry` and `TrainingSession` carry an
  `externalId` plus `@@unique([userId, source, externalId])`, so re-syncing
  the same WHOOP workout or Health Connect record is a no-op rather than a
  duplicate. Postgres treats every `NULL` in a unique index as distinct, so
  the many `MANUAL` rows (`externalId: null`) never collide with each other.
  `RecoveryRecord` doesn't need this — its existing `@@unique([userId, date])`
  already serves as the dedup key for sync purposes.
- **A sync can never overwrite a manual edit.** Enforced once, for every
  provider, in `backend/src/services/externalSyncService.ts`: each upsert
  looks up the existing row by its dedup key and skips entirely if
  `isManuallyEdited` is `true`. This is row-level, not field-level — hand-
  editing one field of a synced day flips the whole row to manually-edited,
  so a later re-sync of that day is skipped in full even though only one
  field changed. Accepted v1 coarseness, not a bug.
- **Raw values are never overwritten by derived ones.** `WeightEntry.weightKg`
  is the literal number the user typed; the moving-average trend line is
  computed fresh on every read from the raw rows (see
  [calculations.md](./calculations.md#weight-trend)) and never written back
  over a raw entry.

## User

Account/profile/preferences. `googleId` is the OAuth identity; `onboardingStatus`
(`PENDING | SKIPPED | COMPLETED`) drives whether `ProtectedRoute` redirects to
`/onboarding`. `estimatedDailyBurnKcal` is a manually-set TDEE baseline used
only by the Fuel energy-balance estimate — there's no formula deriving it from
age/height/weight; the product spec explicitly avoids presenting an estimated
figure as if it were precise, so it's an honest manual input instead of a
guessed one dressed up as a calculation.

## WeightEntry

One row per weigh-in. `note` is free text; nothing else is derived onto this
table — trend/average/rate-of-change all live in `weightService.ts` and are
computed on read.

## NutritionEntry

One row per logged meal (`mealName` optional — "just log the calories" is a
valid use case). Daily/weekly aggregation happens in `nutritionService.ts`,
not on this table.

## TrainingSession + Workout + WorkoutExercise + WorkoutSet

`TrainingSession` is the parent row for every activity type (gym, running,
team sport, match, cycling, walking, recovery session, other). It carries
`trainingLoad`, computed and persisted at write time (not on read) because the
formula that produces it may evolve, and a session's historical load shouldn't
silently change if the formula changes later. `avgHeartRate` is an optional
display-only field populated by WHOOP/Health Connect syncs (real devices
report it); it is never used to derive `intensity` or `trainingLoad` — see
[calculations.md](./calculations.md#whoop-workouts) for why.

Synced sessions (`source = WHOOP` or `HEALTH_CONNECT`) are written directly
via `externalSyncService.ts`, not through `trainingService.createTrainingSession()`
— that function's PR-detection transaction assumes real per-set data, which
neither provider supplies. **PR detection only ever fires for manually-logged
gym sessions.**

`Workout` is a 1:1 child used only when `type = GYM`, holding an ordered list
of `WorkoutExercise` rows, each with its own ordered `WorkoutSet` rows
(reps, weight, optional RPE, `isWarmup` flag). Exercise names are **free
text** — there is no exercise-library table in this build. That's an explicit
MVP simplification, not an oversight: the product spec lists "exercise library
and technique tracking" under *Future Expansion*, separate from MVP scope.
Warmup sets are excluded from every progression calculation (best weight,
estimated 1RM, volume, PR detection).

## MatchDetail

1:1 child used only when `type = MATCH` — opponent, competition, position,
minutes played, result, a 1–10 self-rating, `keyStats` as a flexible
`Record<string, number>` JSON blob (so "goals: 1, assists: 2" doesn't need a
column per possible stat across every sport), injury notes, and a reflection
field.

## RecoveryRecord

One row per calendar day (`@@unique([userId, date])` — logging again the same
day upserts rather than duplicating). `readinessScore` and `readinessLevel`
are computed and persisted at write time by `recoveryService.ts` — see
[calculations.md](./calculations.md#readiness-score) for the exact formula.
`hrv` is HRV-RMSSD in milliseconds (WHOOP's native unit; manual entries use
the same unit for consistency).

**Not every source runs through `computeReadiness()`.** Manually-logged and
Health-Connect-sourced days do (neither has a pre-computed score of its own).
WHOOP-sourced days instead trust WHOOP's own `recovery_score` directly — see
[calculations.md](./calculations.md#readiness-score) for why that asymmetry is
intentional rather than inconsistent.

## IntegrationConnection

One row per user per provider (`@@unique([userId, provider])`), covering both
WHOOP (real OAuth) and Health Connect (no server-side grant — see
[architecture.md](./architecture.md#integrations)) with the same shape so
`GET /api/integrations` can return one uniform list regardless of provider.
OAuth tokens are stored **encrypted at rest** (AES-256-GCM via
`backend/src/lib/crypto.ts`, ciphertext/IV/auth-tag as three sibling columns
per token) — the encryption key lives only in `ENCRYPTION_KEY`, never in the
database, so a database compromise alone doesn't leak tokens.

There is **no foreign key from this table to any data table** — disconnecting
a provider only nulls its token columns and sets `disconnectedAt`; every
`WeightEntry`/`TrainingSession`/`RecoveryRecord` it ever synced is untouched
by construction, not because disconnect logic remembers to preserve it.

## Goal

`type` picks which metric the goal tracks (body weight, calorie intake,
protein intake, training frequency, exercise performance, sport performance,
sleep/recovery, or a custom goal with no auto-tracked metric). `startValue` is
a one-time snapshot taken at creation time — it's what "0% progress" is
measured from, and is deliberately never recomputed after the fact even if the
underlying metric had earlier history. `status` is persisted but recomputed
on every read; see [calculations.md](./calculations.md#goal-status).

`relatedExerciseName` links an `EXERCISE_PERFORMANCE` goal to a specific
`WorkoutExercise.name` string (matched by exact text, since there's no
exercise-library table to reference by ID).

## Achievement

Populated by two triggers, both described in
[calculations.md](./calculations.md#gamification):

- `PERSONAL_RECORD` — created inline inside the same transaction that creates
  a gym `TrainingSession`, if any exercise's estimated 1RM beats every prior
  session's for that exercise name.
- `STREAK` — created after any weight/nutrition/training/recovery write, the
  first time the logging streak or training streak crosses 7, 30, or 100
  consecutive days.

There is intentionally no `WEEKLY_COMPLETION` achievement type wired up yet —
the raw weekly completion percentage is surfaced on the dashboard, but turning
"a perfect week" into a persisted, non-duplicating achievement needs
week-scoped dedup logic that was left out of this pass to keep the
gamification layer deliberately light, per the product spec's own "Light MVP"
categorization for this feature area.

## What's deliberately not a table

**Insights are not persisted anywhere.** `insightsService.getAllInsights()`
recomputes all six rules from live queries on every call. At personal-app data
volumes this is cheap, and it sidesteps an entire class of staleness bugs —
every insight response ships the exact numbers it was computed from, so
"insights explain themselves" (a product requirement) is true by construction
rather than by discipline. An `InsightSnapshot` table would be the natural
next step if this ever needs to run on a schedule for many users.
