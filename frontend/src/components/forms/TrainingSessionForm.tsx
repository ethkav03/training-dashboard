import { useState } from "react";
import { useFieldArray, useForm } from "react-hook-form";
import { ActivityType, MatchResult, type TrainingSessionDto } from "@momentum/shared";
import { Modal } from "../ui/Modal.js";
import { Button } from "../ui/Button.js";
import { useCreateTrainingSession, useUpdateTrainingSession } from "../../hooks/useTraining.js";

const inputClass =
  "w-full rounded-lg border border-hairline bg-page px-2.5 py-1.5 text-sm text-ink-primary outline-none focus:border-series-1";

const ACTIVITY_LABELS: Record<string, string> = {
  GYM: "Gym / strength",
  RUNNING: "Running",
  TEAM_SPORT_TRAINING: "Team sport training",
  MATCH: "Match / competition",
  CYCLING: "Cycling",
  WALKING: "Walking",
  RECOVERY_SESSION: "Recovery session",
  OTHER: "Other",
};

interface SetFormValue {
  reps: number;
  weightKg: number;
  rpe?: number;
  isWarmup: boolean;
}
interface ExerciseFormValue {
  name: string;
  sets: SetFormValue[];
}
interface KeyStatFormValue {
  key: string;
  value: number;
}
interface FormValues {
  type: string;
  date: string;
  durationMin: number;
  intensity: number;
  caloriesBurned?: number;
  notes?: string;
  exercises: ExerciseFormValue[];
  opponent?: string;
  competition?: string;
  position?: string;
  minutesPlayed?: number;
  result?: string;
  performanceRating?: number;
  injuryNotes?: string;
  reflection?: string;
  keyStats: KeyStatFormValue[];
}

function ExerciseEditor({ exIndex, control, register }: { exIndex: number; control: any; register: any }) {
  const { fields, append, remove } = useFieldArray({ control, name: `exercises.${exIndex}.sets` });

  return (
    <div className="rounded-lg border border-hairline p-3">
      <div className="flex items-center gap-2">
        <input
          className={inputClass}
          placeholder="Exercise name"
          {...register(`exercises.${exIndex}.name` as const, { required: true })}
        />
      </div>
      <div className="mt-2 flex flex-col gap-1.5">
        {fields.map((field, setIndex) => (
          <div key={field.id} className="flex items-center gap-1.5">
            <span className="w-5 text-xs text-ink-muted">{setIndex + 1}</span>
            <input
              type="number"
              placeholder="Reps"
              className={inputClass}
              {...register(`exercises.${exIndex}.sets.${setIndex}.reps` as const, { valueAsNumber: true, required: true })}
            />
            <input
              type="number"
              step="0.5"
              placeholder="kg"
              className={inputClass}
              {...register(`exercises.${exIndex}.sets.${setIndex}.weightKg` as const, { valueAsNumber: true, required: true })}
            />
            <input
              type="number"
              step="0.5"
              placeholder="RPE"
              className={`${inputClass} w-16`}
              {...register(`exercises.${exIndex}.sets.${setIndex}.rpe` as const, { valueAsNumber: true })}
            />
            <label className="flex items-center gap-1 text-xs text-ink-secondary">
              <input type="checkbox" {...register(`exercises.${exIndex}.sets.${setIndex}.isWarmup` as const)} />
              warmup
            </label>
            <button
              type="button"
              className="text-xs text-ink-muted hover:text-status-critical"
              onClick={() => remove(setIndex)}
            >
              ✕
            </button>
          </div>
        ))}
      </div>
      <button
        type="button"
        className="mt-2 text-xs text-series-1 hover:underline"
        onClick={() => append({ reps: 8, weightKg: 20, isWarmup: false })}
      >
        + Add set
      </button>
    </div>
  );
}

function buildDefaultValues(session?: TrainingSessionDto): FormValues {
  if (!session) {
    return {
      type: ActivityType.GYM,
      date: new Date().toISOString().slice(0, 16),
      durationMin: 60,
      intensity: 6,
      exercises: [{ name: "", sets: [{ reps: 8, weightKg: 20, isWarmup: false }] }],
      keyStats: [],
    };
  }

  const match = session.matchDetail;
  return {
    type: session.type,
    date: session.date.slice(0, 16),
    durationMin: session.durationMin,
    intensity: session.intensity,
    caloriesBurned: session.caloriesBurned ?? undefined,
    notes: session.notes ?? undefined,
    exercises: session.workout?.exercises.length
      ? session.workout.exercises.map((ex) => ({
          name: ex.name,
          sets: ex.sets.map((s) => ({
            reps: s.reps,
            weightKg: s.weightKg,
            rpe: s.rpe ?? undefined,
            isWarmup: s.isWarmup,
          })),
        }))
      : [{ name: "", sets: [{ reps: 8, weightKg: 20, isWarmup: false }] }],
    opponent: match?.opponent ?? undefined,
    competition: match?.competition ?? undefined,
    position: match?.position ?? undefined,
    minutesPlayed: match?.minutesPlayed ?? undefined,
    result: match?.result ?? undefined,
    performanceRating: match?.performanceRating ?? undefined,
    injuryNotes: match?.injuryNotes ?? undefined,
    reflection: match?.reflection ?? undefined,
    keyStats: match?.keyStats ? Object.entries(match.keyStats).map(([key, value]) => ({ key, value })) : [],
  };
}

export function TrainingSessionForm({ onClose, session }: { onClose: () => void; session?: TrainingSessionDto }) {
  const createMutation = useCreateTrainingSession();
  const updateMutation = useUpdateTrainingSession();
  const isPending = createMutation.isPending || updateMutation.isPending;
  const [submitError, setSubmitError] = useState<string | null>(null);
  const { register, control, handleSubmit, watch } = useForm<FormValues>({
    defaultValues: buildDefaultValues(session),
  });
  const type = watch("type");

  const exercisesArray = useFieldArray({ control, name: "exercises" });
  const keyStatsArray = useFieldArray({ control, name: "keyStats" });

  async function onSubmit(values: FormValues) {
    setSubmitError(null);
    try {
      const payload = {
        type: values.type as never,
        date: new Date(values.date).toISOString(),
        durationMin: Number(values.durationMin),
        intensity: Number(values.intensity),
        caloriesBurned: values.caloriesBurned ? Number(values.caloriesBurned) : undefined,
        notes: values.notes || undefined,
        workout:
          values.type === ActivityType.GYM
            ? {
                exercises: values.exercises
                  .filter((e) => e.name.trim())
                  .map((e, i) => ({
                    name: e.name,
                    orderIndex: i,
                    sets: e.sets.map((s, si) => ({
                      setNumber: si + 1,
                      reps: Number(s.reps),
                      weightKg: Number(s.weightKg),
                      rpe: s.rpe != null && `${s.rpe}` !== "" ? Number(s.rpe) : null,
                      isWarmup: !!s.isWarmup,
                    })),
                  })),
              }
            : undefined,
        matchDetail:
          values.type === ActivityType.MATCH
            ? {
                opponent: values.opponent || null,
                competition: values.competition || null,
                position: values.position || null,
                minutesPlayed: values.minutesPlayed ? Number(values.minutesPlayed) : null,
                result: (values.result as never) || null,
                performanceRating: values.performanceRating ? Number(values.performanceRating) : null,
                keyStats: values.keyStats.length
                  ? Object.fromEntries(values.keyStats.filter((k) => k.key).map((k) => [k.key, Number(k.value)]))
                  : null,
                injuryNotes: values.injuryNotes || null,
                reflection: values.reflection || null,
              }
            : undefined,
      };

      if (session) {
        await updateMutation.mutateAsync({ id: session.id, payload });
      } else {
        await createMutation.mutateAsync(payload);
      }
      onClose();
    } catch {
      setSubmitError("Couldn't save this session — check the fields and try again.");
    }
  }

  return (
    <Modal title={session ? "Edit training session" : "Log training session"} onClose={onClose}>
      <form className="flex max-h-[70vh] flex-col gap-4 overflow-y-auto pr-1" onSubmit={handleSubmit(onSubmit)}>
        <label className="text-sm">
          <span className="mb-1 block text-ink-secondary">Activity type</span>
          <select className={inputClass} {...register("type")}>
            {Object.entries(ACTIVITY_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>

        <div className="grid grid-cols-2 gap-3">
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Date & time</span>
            <input className={inputClass} type="datetime-local" {...register("date", { required: true })} />
          </label>
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Duration (min)</span>
            <input className={inputClass} type="number" {...register("durationMin", { valueAsNumber: true, required: true })} />
          </label>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Intensity (1-10)</span>
            <input
              className={inputClass}
              type="number"
              min={1}
              max={10}
              {...register("intensity", { valueAsNumber: true, required: true })}
            />
          </label>
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Calories burned (optional)</span>
            <input className={inputClass} type="number" {...register("caloriesBurned", { valueAsNumber: true })} />
          </label>
        </div>

        {type === ActivityType.GYM && (
          <div className="flex flex-col gap-3">
            <span className="text-sm text-ink-secondary">Exercises</span>
            {exercisesArray.fields.map((field, i) => (
              <ExerciseEditor key={field.id} exIndex={i} control={control} register={register} />
            ))}
            <button
              type="button"
              className="self-start text-xs text-series-1 hover:underline"
              onClick={() => exercisesArray.append({ name: "", sets: [{ reps: 8, weightKg: 20, isWarmup: false }] })}
            >
              + Add exercise
            </button>
          </div>
        )}

        {type === ActivityType.MATCH && (
          <div className="flex flex-col gap-3 rounded-lg border border-hairline p-3">
            <div className="grid grid-cols-2 gap-3">
              <label className="text-sm">
                <span className="mb-1 block text-ink-secondary">Opponent</span>
                <input className={inputClass} {...register("opponent")} />
              </label>
              <label className="text-sm">
                <span className="mb-1 block text-ink-secondary">Competition</span>
                <input className={inputClass} {...register("competition")} />
              </label>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <label className="text-sm">
                <span className="mb-1 block text-ink-secondary">Position</span>
                <input className={inputClass} {...register("position")} />
              </label>
              <label className="text-sm">
                <span className="mb-1 block text-ink-secondary">Minutes played</span>
                <input className={inputClass} type="number" {...register("minutesPlayed", { valueAsNumber: true })} />
              </label>
              <label className="text-sm">
                <span className="mb-1 block text-ink-secondary">Result</span>
                <select className={inputClass} {...register("result")}>
                  <option value="">—</option>
                  <option value={MatchResult.WIN}>Win</option>
                  <option value={MatchResult.LOSS}>Loss</option>
                  <option value={MatchResult.DRAW}>Draw</option>
                </select>
              </label>
            </div>
            <label className="text-sm">
              <span className="mb-1 block text-ink-secondary">Performance rating (1-10)</span>
              <input className={inputClass} type="number" min={1} max={10} {...register("performanceRating", { valueAsNumber: true })} />
            </label>

            <div>
              <span className="mb-1 block text-sm text-ink-secondary">Key stats</span>
              {keyStatsArray.fields.map((field, i) => (
                <div key={field.id} className="mb-1.5 flex items-center gap-1.5">
                  <input className={inputClass} placeholder="e.g. goals" {...register(`keyStats.${i}.key` as const)} />
                  <input
                    className={inputClass}
                    type="number"
                    placeholder="value"
                    {...register(`keyStats.${i}.value` as const, { valueAsNumber: true })}
                  />
                  <button
                    type="button"
                    className="text-xs text-ink-muted hover:text-status-critical"
                    onClick={() => keyStatsArray.remove(i)}
                  >
                    ✕
                  </button>
                </div>
              ))}
              <button
                type="button"
                className="text-xs text-series-1 hover:underline"
                onClick={() => keyStatsArray.append({ key: "", value: 0 })}
              >
                + Add stat
              </button>
            </div>

            <label className="text-sm">
              <span className="mb-1 block text-ink-secondary">Injury / pain notes</span>
              <input className={inputClass} {...register("injuryNotes")} />
            </label>
            <label className="text-sm">
              <span className="mb-1 block text-ink-secondary">Post-match reflection</span>
              <textarea className={inputClass} rows={2} {...register("reflection")} />
            </label>
          </div>
        )}

        <label className="text-sm">
          <span className="mb-1 block text-ink-secondary">Notes</span>
          <textarea className={inputClass} rows={2} {...register("notes")} />
        </label>

        {submitError && <p className="text-sm text-status-critical">{submitError}</p>}

        <Button type="submit" disabled={isPending}>
          {isPending ? "Saving..." : session ? "Save changes" : "Save session"}
        </Button>
      </form>
    </Modal>
  );
}
