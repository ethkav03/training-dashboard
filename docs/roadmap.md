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

## Explicitly out of scope for this build

Called out here so they don't get silently assumed as "missing bugs" later —
these were deliberate scope cuts agreed at the start of the build, not gaps:

- **Real Google Fit / WHOOP / MyFitnessPal integrations.** The data model
  carries `source` and `isManuallyEdited` on every loggable record
  specifically so these can be added later without a migration, but no
  OAuth/sync code exists. All data in this build is manually entered.
- **Push/email notifications** (goal reminders, sync-failure alerts, weekly
  summaries) — no notification infrastructure at all.
- **Native mobile app** — the web app is responsive/mobile-first, but there's
  no Capacitor/React Native wrapper.
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
  first created, not when an existing session is edited via `PUT`.

## Possible next steps (not committed to)

Roughly in order of how directly they build on what exists:

1. Weekly/monthly calendar grid view for Timeline, beyond the current
   week-at-a-time list.
2. `WEEKLY_COMPLETION` achievement type with week-scoped dedup.
3. Real integration for one provider (Google Fit is the most likely first
   candidate given it shares the existing Google OAuth client) — would mean
   building the sync-status UI, conflict-resolution rules, and a background
   sync job the spec describes in section 15/24, none of which exist yet.
4. Exercise library table, referenced by ID instead of free-text name
   matching — would make `EXERCISE_PERFORMANCE` goals and PR detection exact
   rather than string-matched.
