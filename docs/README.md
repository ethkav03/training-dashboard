# Momentum — Documentation

Implementation documentation for the Momentum training & performance dashboard.
For "how do I run this locally," see the [root README](../README.md) instead —
this folder is about how the system is built and why, not how to start it.

- [architecture.md](./architecture.md) — stack, monorepo layout, request flow, auth flow, containerization
- [data-model.md](./data-model.md) — every Prisma entity, its fields, and why it's shaped that way
- [calculations.md](./calculations.md) — every derived/computed value in the app and its exact formula
- [api-reference.md](./api-reference.md) — REST endpoint reference by resource

## Scope note

This build implements the product spec's Phase 1 (Foundation), Phase 2 (Manual
Tracking), and the core of Phase 4 (Progress: charts, timeline, insights) — all
five pillars (Body, Fuel, Training, Recovery, Goals) with manual entry, plus a
light gamification layer (streaks, PRs). **Real external integrations (Google
Fit, WHOOP, MyFitnessPal) are explicitly out of scope for this build** — the
data model carries a `source` enum and `isManuallyEdited` flag on every
loggable record specifically so those integrations can be added later without
a schema migration, but no OAuth/sync code exists yet. Push notifications,
native mobile apps, and product-usage telemetry are likewise deferred.
