import type { GoalDto } from "@momentum/shared";
import { Card } from "../ui/Card.js";
import { useDeleteGoal, useUpdateGoal } from "../../hooks/useGoals.js";

const STATUS_STYLES: Record<GoalDto["status"], { color: string; label: string }> = {
  ON_TRACK: { color: "var(--status-good)", label: "On track" },
  AT_RISK: { color: "var(--status-warning)", label: "At risk" },
  ACHIEVED: { color: "var(--status-good)", label: "Achieved" },
  PAUSED: { color: "var(--text-muted)", label: "Paused" },
};

const TYPE_LABELS: Record<string, string> = {
  BODY_WEIGHT: "Body weight",
  CALORIE_INTAKE: "Calorie intake",
  PROTEIN_INTAKE: "Protein intake",
  TRAINING_FREQUENCY: "Training frequency",
  EXERCISE_PERFORMANCE: "Exercise performance",
  SPORT_PERFORMANCE: "Sport performance",
  SLEEP_RECOVERY: "Sleep & recovery",
  CUSTOM: "Custom",
};

export function GoalCard({ goal }: { goal: GoalDto }) {
  const updateMutation = useUpdateGoal();
  const deleteMutation = useDeleteGoal();
  const style = STATUS_STYLES[goal.status];
  const clampedProgress = goal.progressPercent != null ? Math.max(0, Math.min(100, goal.progressPercent)) : null;

  return (
    <Card>
      <div className="flex items-start justify-between gap-2">
        <div>
          <div className="text-xs text-ink-muted">{TYPE_LABELS[goal.type] ?? goal.type}</div>
          <div className="font-medium text-ink-primary">{goal.title}</div>
        </div>
        <span className="text-xs font-medium" style={{ color: style.color }}>
          {style.label}
        </span>
      </div>

      <div className="mt-3">
        <div className="h-2 w-full rounded-full bg-page">
          {clampedProgress != null && (
            <div
              className="h-2 rounded-full transition-all"
              style={{ width: `${clampedProgress}%`, backgroundColor: style.color }}
            />
          )}
        </div>
        <div className="mt-1 flex justify-between text-xs text-ink-secondary">
          <span>
            {goal.currentValue != null ? Math.round(goal.currentValue * 10) / 10 : "—"}
            {goal.targetValue != null && ` / ${goal.targetValue}`} {goal.targetUnit ?? ""}
          </span>
          <span>{clampedProgress != null ? `${clampedProgress}%` : "no data yet"}</span>
        </div>
      </div>

      <div className="mt-3 flex items-center gap-3 text-xs">
        {goal.status !== "ACHIEVED" && (
          <button
            className="text-ink-muted hover:text-ink-primary"
            onClick={() =>
              updateMutation.mutate({ id: goal.id, patch: { status: goal.status === "PAUSED" ? "ON_TRACK" : "PAUSED" } })
            }
          >
            {goal.status === "PAUSED" ? "Resume" : "Pause"}
          </button>
        )}
        <button className="text-ink-muted hover:text-status-critical" onClick={() => deleteMutation.mutate(goal.id)}>
          Delete
        </button>
      </div>
    </Card>
  );
}
