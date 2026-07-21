import { useState } from "react";
import { Link } from "react-router-dom";
import {
  useDeleteTrainingSession,
  useExerciseNames,
  useLoadSummary,
  useTrainingSessions,
} from "../hooks/useTraining.js";
import { TrainingSessionForm } from "../components/forms/TrainingSessionForm.js";
import { Button } from "../components/ui/Button.js";
import { Card, CardTitle } from "../components/ui/Card.js";

const ACTIVITY_LABELS: Record<string, string> = {
  GYM: "Gym",
  RUNNING: "Running",
  TEAM_SPORT_TRAINING: "Team sport training",
  MATCH: "Match",
  CYCLING: "Cycling",
  WALKING: "Walking",
  RECOVERY_SESSION: "Recovery session",
  OTHER: "Other",
};

export function TrainingPage() {
  const { data: sessions, isLoading } = useTrainingSessions();
  const { data: loadSummary } = useLoadSummary();
  const { data: exerciseNames } = useExerciseNames();
  const deleteMutation = useDeleteTrainingSession();
  const [showForm, setShowForm] = useState(false);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Training</h1>
        <Button size="sm" onClick={() => setShowForm(true)}>
          Log session
        </Button>
      </div>

      {loadSummary && (
        <div className="grid grid-cols-3 gap-3">
          <Card>
            <CardTitle>Weekly load</CardTitle>
            <div className="mt-1 text-2xl font-semibold">{loadSummary.weeklyLoad}</div>
          </Card>
          <Card>
            <CardTitle>Sessions this week</CardTitle>
            <div className="mt-1 text-2xl font-semibold">{loadSummary.sessionsThisWeek}</div>
          </Card>
          <Card>
            <CardTitle>Load ratio (7d:28d)</CardTitle>
            <div className="mt-1 text-2xl font-semibold">{loadSummary.acwr ?? "—"}</div>
            {loadSummary.acwr != null && loadSummary.acwr > 1.5 && (
              <div className="text-xs text-status-serious">Load rising quickly vs. recent average</div>
            )}
          </Card>
        </div>
      )}

      {!!exerciseNames?.length && (
        <Card>
          <CardTitle>Exercise progression</CardTitle>
          <div className="mt-2 flex flex-wrap gap-2">
            {exerciseNames.map((name) => (
              <Link
                key={name}
                to={`/training/exercises/${encodeURIComponent(name)}`}
                className="rounded-full border border-hairline px-3 py-1 text-xs text-ink-secondary hover:text-ink-primary hover:border-series-1"
              >
                {name}
              </Link>
            ))}
          </div>
        </Card>
      )}

      <Card>
        <CardTitle>Recent sessions</CardTitle>
        <div className="mt-3 flex flex-col divide-y divide-hairline">
          {!isLoading && sessions?.length === 0 && (
            <p className="py-6 text-center text-sm text-ink-muted">No sessions logged yet.</p>
          )}
          {sessions?.map((session) => (
            <div key={session.id} className="py-3 text-sm">
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-medium text-ink-primary">{ACTIVITY_LABELS[session.type] ?? session.type}</span>
                  <span className="ml-2 text-xs text-ink-muted">
                    {new Date(session.date).toLocaleDateString()} · {session.durationMin} min · intensity{" "}
                    {session.intensity}/10 · load {session.trainingLoad}
                  </span>
                </div>
                <button
                  className="text-xs text-ink-muted hover:text-status-critical"
                  onClick={() => deleteMutation.mutate(session.id)}
                >
                  Remove
                </button>
              </div>
              {session.workout && (
                <ul className="mt-1.5 text-xs text-ink-secondary">
                  {session.workout.exercises.map((ex) => (
                    <li key={ex.name}>
                      {ex.name}: {ex.sets.map((s) => `${s.reps}x${s.weightKg}kg`).join(", ")}
                    </li>
                  ))}
                </ul>
              )}
              {session.matchDetail && (
                <div className="mt-1.5 text-xs text-ink-secondary">
                  {session.matchDetail.opponent && `vs ${session.matchDetail.opponent} · `}
                  {session.matchDetail.result && `${session.matchDetail.result} · `}
                  {session.matchDetail.performanceRating != null &&
                    `rating ${session.matchDetail.performanceRating}/10`}
                </div>
              )}
              {session.notes && <p className="mt-1.5 text-xs text-ink-secondary">{session.notes}</p>}
            </div>
          ))}
        </div>
      </Card>

      {showForm && <TrainingSessionForm onClose={() => setShowForm(false)} />}
    </div>
  );
}
