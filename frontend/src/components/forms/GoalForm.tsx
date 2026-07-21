import { useState } from "react";
import { GoalDirection, GoalType } from "@momentum/shared";
import { Modal } from "../ui/Modal.js";
import { Button } from "../ui/Button.js";
import { useCreateGoal } from "../../hooks/useGoals.js";
import { useExerciseNames } from "../../hooks/useTraining.js";

const inputClass =
  "w-full rounded-lg border border-hairline bg-page px-3 py-2 text-sm text-ink-primary outline-none focus:border-series-5";

const TYPE_OPTIONS: { value: string; label: string; defaultUnit: string }[] = [
  { value: GoalType.BODY_WEIGHT, label: "Body weight", defaultUnit: "kg" },
  { value: GoalType.CALORIE_INTAKE, label: "Calorie intake", defaultUnit: "kcal/day" },
  { value: GoalType.PROTEIN_INTAKE, label: "Protein intake", defaultUnit: "g/day" },
  { value: GoalType.TRAINING_FREQUENCY, label: "Training frequency", defaultUnit: "sessions/wk" },
  { value: GoalType.EXERCISE_PERFORMANCE, label: "Exercise performance", defaultUnit: "kg" },
  { value: GoalType.SPORT_PERFORMANCE, label: "Sport performance", defaultUnit: "rating" },
  { value: GoalType.SLEEP_RECOVERY, label: "Sleep & recovery", defaultUnit: "hours" },
  { value: GoalType.CUSTOM, label: "Custom", defaultUnit: "" },
];

export function GoalForm({ onClose }: { onClose: () => void }) {
  const [type, setType] = useState<string>(GoalType.BODY_WEIGHT);
  const [title, setTitle] = useState("");
  const [targetValue, setTargetValue] = useState("");
  const [targetUnit, setTargetUnit] = useState("kg");
  const [direction, setDirection] = useState<string>(GoalDirection.DECREASE);
  const [targetDate, setTargetDate] = useState("");
  const [relatedExerciseName, setRelatedExerciseName] = useState("");
  const mutation = useCreateGoal();
  const { data: exerciseNames } = useExerciseNames();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    await mutation.mutateAsync({
      type,
      title,
      targetValue: targetValue ? Number(targetValue) : undefined,
      targetUnit: targetUnit || undefined,
      direction,
      targetDate: targetDate ? new Date(targetDate).toISOString() : undefined,
      relatedExerciseName: type === GoalType.EXERCISE_PERFORMANCE ? relatedExerciseName : undefined,
    });
    onClose();
  }

  return (
    <Modal title="New goal" onClose={onClose}>
      <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
        <label className="text-sm">
          <span className="mb-1 block text-ink-secondary">Goal type</span>
          <select
            className={inputClass}
            value={type}
            onChange={(e) => {
              const opt = TYPE_OPTIONS.find((o) => o.value === e.target.value);
              setType(e.target.value);
              setTargetUnit(opt?.defaultUnit ?? "");
            }}
          >
            {TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm">
          <span className="mb-1 block text-ink-secondary">Title</span>
          <input
            required
            autoFocus
            className={inputClass}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Reach 80kg"
          />
        </label>

        {type === GoalType.EXERCISE_PERFORMANCE && (
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Exercise</span>
            <select
              required
              className={inputClass}
              value={relatedExerciseName}
              onChange={(e) => setRelatedExerciseName(e.target.value)}
            >
              <option value="">Select an exercise...</option>
              {exerciseNames?.map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
        )}

        <div className="grid grid-cols-2 gap-3">
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Target value</span>
            <input className={inputClass} type="number" step="any" value={targetValue} onChange={(e) => setTargetValue(e.target.value)} />
          </label>
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Unit</span>
            <input className={inputClass} value={targetUnit} onChange={(e) => setTargetUnit(e.target.value)} />
          </label>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Direction</span>
            <select className={inputClass} value={direction} onChange={(e) => setDirection(e.target.value)}>
              <option value={GoalDirection.DECREASE}>Decrease</option>
              <option value={GoalDirection.INCREASE}>Increase</option>
            </select>
          </label>
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Target date (optional)</span>
            <input className={inputClass} type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
          </label>
        </div>

        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Saving..." : "Create goal"}
        </Button>
      </form>
    </Modal>
  );
}
