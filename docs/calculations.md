# Calculations Reference

Every derived/computed value in Momentum, its exact formula, and the file it
lives in. The guiding rule throughout (per the product spec): **never present
an estimate as false precision, and never make a number the reader can't
trace back to its inputs.**

---

## Weight trend

`backend/src/services/weightService.ts`

### Moving average

A trailing **7-day mean** computed fresh on every read over the raw
`WeightEntry` rows — never persisted, never overwrites a raw value.

For each entry at date *d*, average the `weightKg` of every entry with
`date > d - 7 days` and `date <= d`. Same-calendar-day duplicates collapse to
the latest write for that day (last write wins on the trend line only — the
raw list still shows every entry).

### Rate of change (kg/week)

An ordinary least-squares linear regression over the raw series (x = days
since the first entry in the window, y = weight), slope expressed per week:

```
slope_per_day = (n·Σxy − Σx·Σy) / (n·Σx² − (Σx)²)
rate_per_week = slope_per_day × 7
```

**Guard:** if the series spans less than **3 days**, this returns `null`
instead of a number. Without this guard, two entries logged a few hours apart
produce a technically-real but meaningless extrapolation — dividing a small
weight delta by a near-zero time delta and multiplying by 7 can produce a rate
in the thousands of kg/week. This was caught during manual testing, not
designed upfront, and is exactly the kind of "false precision" the spec warns
against.

---

## Fuel / energy balance

`backend/src/services/nutritionService.ts`

```
totalCalories   = Σ NutritionEntry.calories for the day
estimatedBurn   = user.estimatedDailyBurnKcal (manual baseline, or 0 if unset)
                  + Σ TrainingSession.caloriesBurned for the day
balanceKcal     = totalCalories − estimatedBurn   (null if no baseline is set)
```

`estimatedDailyBurnKcal` is a **manual** Settings field, not derived from
age/height/weight/activity — the product spec is explicit that estimated
expenditure must never be presented with false precision, so this build asks
the user for their own number rather than computing one that looks more
authoritative than it is. The response always carries `isEstimate: true`.

### Consumed-vs-burned series (day/week/month/year)

`nutritionService.getEnergyBalanceSeries()`, backing the Fuel tab's trend
graph and `GET /nutrition/energy-balance?granularity=`.

Rather than looping the per-day summary function above over every day in the
requested range (which would mean hundreds of queries for a multi-year "year"
view), this walks two flat range queries (`NutritionEntry`, `TrainingSession`)
once and buckets them in memory by the requested granularity:

```
bucketKey(date, "day")   = the date itself
bucketKey(date, "week")  = that date's Monday (Monday-start weeks)
bucketKey(date, "month") = "YYYY-MM"
bucketKey(date, "year")  = "YYYY"
```

Default lookback per granularity: **day** → last 30 days, **week** → last 12
weeks, **month** → last 12 months, **year** → last 5 years.

Critically, the bucketing walks **every calendar day** in the lookback range
(not just days with a logged entry) and adds the baseline burn for each one —
otherwise a week or month's total burn would silently undercount by excluding
days the user just didn't log a meal on. The current (most recent, still
in-progress) bucket is therefore intentionally partial — e.g. a "this week"
bucket on a Tuesday only accumulates baseline burn for Monday + Tuesday, not
the full 7 days, since the rest of the week hasn't happened yet. This was
verified arithmetically during manual testing (baseline × elapsed-days-so-far
matched the API response exactly for week/month/year buckets), not assumed.

---

## Training load

`backend/src/services/trainingService.ts`

```
trainingLoad = durationMin × intensity      (intensity = subjective 1–10 RPE)
```

A simple session-RPE (Foster's method) load score — deliberately the
simplest formula that's still explainable in one sentence, computed and
**persisted at write time** (not recomputed on read) so a session's
historical load is stable even if this formula changes later.

### Weekly load summary / ACWR

```
weeklyLoad        = Σ trainingLoad for sessions in the last 7 days
acuteAvgDaily      = weeklyLoad / 7
chronicAvgDaily    = Σ trainingLoad for sessions in the last 28 days / 28
acwr              = acuteAvgDaily / chronicAvgDaily     (null if chronic = 0)
```

Acute:chronic workload ratio — a standard sports-science heuristic for "is
load rising too fast relative to what the body is adapted to." The dashboard
flags `acwr > 1.5` as a spike worth noting.

---

## Estimated one-rep max & exercise progression

`backend/src/services/trainingService.ts`

```
estimated1RM = weightKg × (1 + reps / 30)         (Epley formula)
```

Computed per **non-warmup** `WorkoutSet`. For a given exercise across
sessions (`getExerciseProgression`):

- `bestWeightKg` = max `weightKg` among that session's working sets
- `estimated1RM` = max Epley result among that session's working sets
- `volume` = Σ `reps × weightKg` among that session's working sets
- `isPr` = this session's `estimated1RM` exceeds the running best across all
  **earlier** sessions for that exact exercise name (walking sessions in date
  order, tracking a running maximum)

The Epley formula was chosen over more complex alternatives (Brzycki, Lombardi)
because it's linear and trivially explainable — matching the spec's "insights
must explain the underlying data" principle applied to progression math too.

---

## Readiness score

`backend/src/services/recoveryService.ts` — `computeReadiness()`

A weighted 0–100 score from whichever inputs the user actually logged that
day, **renormalized over just those components** so a missing metric (no HRV
device, didn't log energy) doesn't silently drag the score toward zero.

| Component | Score formula | Weight |
|---|---|---|
| Sleep duration | `clamp(sleepHours / 8 × 100, 0, 100)` | 0.25 |
| Sleep quality (1–5) | `clamp(sleepQuality / 5 × 100, 0, 100)` | 0.15 |
| Resting HR | vs. the user's own 30-day trailing average: `clamp(100 − max(0, hr − baselineHr) × 5, 0, 100)` | 0.15 |
| HRV | vs. 30-day trailing average: `clamp(hrv / baselineHrv × 100, 0, 100)` | 0.15 |
| Soreness (1–5, 5 = most sore) | `clamp((6 − soreness) / 5 × 100, 0, 100)` | 0.15 |
| Energy (1–5) | `clamp(energy / 5 × 100, 0, 100)` | 0.15 |

```
score = Σ(component_value × weight) / Σ(weight of components with data)
```

Resting HR and HRV are scored **relative to the user's own baseline**, not a
fixed population number — a resting HR of 55 means something different for
different people, so the comparison is always against that same user's
trailing 30-day average (excluding the day being scored). If there's no
baseline yet (first week of use), that component is simply excluded from the
weighted average rather than guessed at.

```
level = HIGH   if score >= 75
        MODERATE if 50 <= score < 75
        LOW    if score < 50
```

Each level maps to a fixed recommendation string (`recoveryService.ts`,
`RECOMMENDATIONS`) — presented as decision support, explicitly not medical
advice or a diagnosis, per the product spec.

**This formula only runs for manually-logged and Health-Connect-sourced days.**
WHOOP-sourced recovery rows instead trust WHOOP's own `recovery_score` (0–100)
directly, mapped to HIGH/MODERATE/LOW via the same thresholds
(`scoreToReadinessLevel()`, shared so both paths use identical cutoffs).
Reasoning: WHOOP's score already blends HRV + resting HR + sleep quality
against WHOOP's own sensor-calibrated baseline — re-running our simpler
formula on top of a *subset* of that same signal (WHOOP gives us no
soreness/energy at all) would produce a lower-confidence number dressed up as
equivalent to a fully-manual entry, which is exactly the "false precision"
this whole document argues against elsewhere. Health Connect, by contrast,
ships no pre-computed score of its own — raw sensor data only — so our
formula is the only thing that *can* produce one for it, and doing so scores
those days on the same yardstick as manual entries.

**`sleepScore` and `strain` are pass-through headline metrics, not inputs to
`computeReadiness()`.** WHOOP's own app shows three numbers every morning —
recovery, sleep, strain — and `RecoveryRecord` stores all three as independent
fields rather than folding sleep/strain into the readiness formula above. For
WHOOP-sourced rows:

```
sleepScore = sleep.score.sleep_performance_percentage        (0-100, WHOOP's own sleep score)
strain     = previous completed cycle's score.strain, rounded to 1 decimal  ("yesterday's strain")
```

The strain lookup is deliberately the *previous* cycle, not the current one:
WHOOP's own convention is that "yesterday's strain" is the exertion figure
that's actually final by the time you check recovery in the morning — the
current cycle is still accumulating strain throughout today, so showing it
next to this morning's recovery score would pair a settled number with a
half-finished one. `whoopService.ts` fetches `/v2/cycle` alongside recovery/
sleep/workout, sorts cycles chronologically, and for each recovery's
`cycle_id` looks up the cycle immediately before it in that ordering — not
some fixed "cycle_id − 1," since cycle ids aren't guaranteed contiguous. If
that previous cycle isn't `SCORED` yet (still in progress, or the very first
cycle on record), `strain` is left `null` rather than guessed at.

Manual entries can log `sleepScore`/`strain` directly too (a user without a
wearable who still wants to track the same three numbers by hand), and both
are simply `null` for Health-Connect-sourced rows, which have no equivalent
metric to report.

---

## WHOOP workouts

`backend/src/services/whoopService.ts`

```
durationMin    = (workout.end − workout.start) in minutes
intensity      = clamp(round(workout.score.strain / 21 × 10), 1, 10)
caloriesBurned = round(workout.score.kilojoule × 0.239006)     (kJ → kcal)
avgHeartRate   = round(workout.score.average_heart_rate)        (display only)
trainingLoad   = durationMin × intensity                        (same formula as manual sessions)
```

WHOOP's strain is its own 0–21 whole-day-relative exertion scale; normalizing
it into our 1–10 intensity is a real unit conversion, not a guess.
`avgHeartRate` is stored and shown as honest supplementary context but is
**never** used to derive `intensity` — estimating perceived exertion from
heart rate alone (e.g. against an age-estimated max HR) would be exactly the
kind of invented-looking precision this document argues against; Health
Connect-sourced sessions instead get a fixed placeholder `intensity = 5` with
a UI prompt to correct it, for the same reason.

Sport is matched by WHOOP's human-readable `sport_name` (lowercased) against a
fixed lookup table (`SPORT_NAME_TO_ACTIVITY_TYPE`), not by WHOOP's numeric
`sport_id` — WHOOP doesn't publish a stable public enum for the IDs, so
matching on the documented, self-describing name is the more robust choice.
Team sports (soccer, basketball, etc.) always map to `TEAM_SPORT_TRAINING`,
never `MATCH` — WHOOP has no opponent/result data, so `MatchDetail` couldn't
be populated anyway; the user can manually convert an important session
afterward. Unmapped sports default to `OTHER`. Synced workouts never carry
nested `Workout`/`WorkoutSet` rows (WHOOP has no per-set data), so **PR
detection never fires for synced sessions** — only for manually-logged gym
workouts with real set data.

WHOOP's recovery and sleep records are merged into one `RecoveryRecord` row
per day, keyed by the **recovery's** `created_at` (truncated to the day) —
not the sleep's own start/end — because a recovery is only generated once its
paired sleep closes, so `created_at` reliably lands on the same calendar day
as that sleep, giving both records one consistent day bucket to merge into.
Sleep hours are computed as light + slow-wave + REM sleep time (excluding
awake time within the sleep window), not total time in bed.

---

## Goal progress & status

`backend/src/services/goalService.ts`

### Current value (type-aware)

| Goal type | Current value = |
|---|---|
| `BODY_WEIGHT` | latest `WeightEntry.weightKg` |
| `CALORIE_INTAKE` | trailing 7-day average of daily total calories |
| `PROTEIN_INTAKE` | trailing 7-day average of daily total protein (g) |
| `TRAINING_FREQUENCY` | count of `TrainingSession` rows in the last 7 days |
| `EXERCISE_PERFORMANCE` | best estimated 1RM ever logged for `relatedExerciseName` |
| `SPORT_PERFORMANCE` | average `performanceRating` over the last 5 matches |
| `SLEEP_RECOVERY` | trailing 7-day average `sleepHours` |
| `CUSTOM` | `null` — no auto-tracked metric; status only changes via manual pause/resume |

### Progress percent

```
range      = direction == DECREASE ? startValue − targetValue
                                    : targetValue − startValue
progressed = direction == DECREASE ? startValue − currentValue
                                    : currentValue − startValue
progressPercent = progressed / range × 100
```

`startValue` is a one-time snapshot taken when the goal is created (so a
weight-loss goal always starts at a clean 0%, regardless of how much history
already existed).

### Status

Recomputed on **every read** (cheap at personal-app scale), persisted only
when it changes:

```
if status was manually set to PAUSED           -> stays PAUSED (no auto-recompute)
if progressPercent == null                     -> keep last computed status
if progressPercent >= 100                      -> ACHIEVED (achievedAt set once)
else if targetDate is set:
    expectedPercent = min(100, elapsed_time / total_time × 100)
    ON_TRACK if progressPercent >= expectedPercent − 10, else AT_RISK
else (no target date):
    ON_TRACK if progressPercent >= 70, else AT_RISK
```

The `-10` tolerance band against the expected-progress-by-target-date curve
avoids flipping a goal to "at risk" over noise on any single day.

---

## Insights

`backend/src/services/insightsService.ts`

Six independent, deterministic rules. Each is only surfaced if it clears a
**materiality threshold** — the point of the thresholds is to keep the list
free of noise, not to hide unflattering numbers:

| Rule | Compares | Threshold to surface |
|---|---|---|
| Weight trend | 7-day avg weight vs. the 7-day avg from ~3 weeks ago | \|Δ\| > 0.2 kg |
| Training volume | this week's total load vs. avg of the preceding 4 weeks | \|% change\| > 15% |
| Sleep vs. baseline | 7-day avg sleep vs. 30-day avg sleep | \|Δ minutes\| > 30 |
| Calorie intake vs. goal | 7-day avg calories vs. active `CALORIE_INTAKE` goal target | \|% diff\| > 5% |
| Protein intake vs. goal | 7-day avg protein vs. active `PROTEIN_INTAKE` goal target | \|% diff\| > 5% |
| Readiness trend | avg readiness score over the last 3 logged days | average < 50 |

Every returned insight includes `metrics: { current, previous, unit,
windowDays }` — the exact numbers behind the headline, always. Insights are
**not persisted**; they're recomputed on every request (see
[data-model.md](./data-model.md#whats-deliberately-not-a-table) for why).

---

## Gamification

`backend/src/services/gamificationService.ts`

### Streaks

Both walk backward from today counting consecutive calendar days with
qualifying activity, stopping at the first gap:

- **Logging streak** — a day counts if *any* of WeightEntry, NutritionEntry,
  TrainingSession, or RecoveryRecord has an entry that day.
- **Training streak** — a day counts only if a TrainingSession exists that
  day.

### Weekly completion score

```
weeklyCompletionScore = round(distinct days with any log in the last 7 days / 7 × 100)
```

### Streak milestone achievements

After every weight/nutrition/training/recovery write, both streaks are
recomputed; if either streak's value is **exactly** 7, 30, or 100, a `STREAK`
achievement is created — unless one with the same title already exists for
that user (dedup key: `userId + type=STREAK + title`), so a currently-active
streak doesn't spam duplicate achievements on every subsequent day. Breaking
and rebuilding a streak to the same milestone later creates a fresh
achievement, since the title alone doesn't encode *when* it was earned.

### Personal records

Computed inline, inside the same database transaction that creates a `GYM`
`TrainingSession`: for each exercise in the new session, its best estimated
1RM (via the Epley formula above) is compared against the best estimated 1RM
across **every other session** for that exact exercise name for that user. If
the new session wins, a `PERSONAL_RECORD` achievement is created immediately.

**Scope note:** PR detection runs only on session *creation*, not on edits —
editing a past session's weights won't retroactively create or remove a PR
achievement. This is a deliberate scope cut, not an oversight.
