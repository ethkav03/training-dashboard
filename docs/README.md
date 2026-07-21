# Momentum — Documentation

Implementation documentation for the Momentum training & performance dashboard.
For "how do I run this locally," see the [root README](../README.md) instead —
this folder is about how the system is built and why, not how to start it.

- [architecture.md](./architecture.md) — stack, monorepo layout, request flow, auth flow, containerization, integrations, native Android app
- [data-model.md](./data-model.md) — every Prisma entity, its fields, and why it's shaped that way
- [calculations.md](./calculations.md) — every derived/computed value in the app and its exact formula
- [api-reference.md](./api-reference.md) — REST endpoint reference by resource
- [roadmap.md](./roadmap.md) — sprint-by-sprint status of what's built vs. planned vs. explicitly out of scope

## Scope note

This build implements the product spec's Phase 1 (Foundation), Phase 2 (Manual
Tracking), and the core of Phase 4 (Progress: charts, timeline, insights) — all
five pillars (Body, Fuel, Training, Recovery, Goals) with manual entry, a light
gamification layer (streaks, PRs), **and** real external integrations:
**WHOOP** (OAuth2, recovery/sleep/workout sync) and **Android Health Connect**
via a companion native Kotlin app (on-device permission flow, manual sync, and
`WorkManager` background sync). See
[roadmap.md](./roadmap.md#sprint-status) for the sprint each of these landed
in and their exact verification status.

**Google Fit and MyFitnessPal are the two integrations that stayed out of
scope**, not by choice but because neither is buildable: Google Fit's REST
API has had developer sign-ups frozen since May 2024 (full sunset end of
2026), and MyFitnessPal's API is closed to new developer applications.
Google-side health data comes from Health Connect instead (see above); meals
are logged manually.

Push notifications and product-usage telemetry are still deferred — see
[roadmap.md](./roadmap.md#explicitly-out-of-scope-for-this-build) for the
full list of deliberate scope cuts and why each one was made.
