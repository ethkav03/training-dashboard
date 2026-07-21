import { useState } from "react";
import { useGoals } from "../hooks/useGoals.js";
import { GoalCard } from "../components/cards/GoalCard.js";
import { GoalForm } from "../components/forms/GoalForm.js";
import { Button } from "../components/ui/Button.js";

export function GoalsPage() {
  const { data: goals, isLoading } = useGoals();
  const [showForm, setShowForm] = useState(false);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Goals</h1>
        <Button size="sm" onClick={() => setShowForm(true)}>
          New goal
        </Button>
      </div>

      {!isLoading && goals?.length === 0 && (
        <p className="py-8 text-center text-sm text-ink-muted">
          No goals yet — set one to turn your logging into progress you can track.
        </p>
      )}

      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        {goals?.map((goal) => (
          <GoalCard key={goal.id} goal={goal} />
        ))}
      </div>

      {showForm && <GoalForm onClose={() => setShowForm(false)} />}
    </div>
  );
}
