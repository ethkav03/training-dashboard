import { useState } from "react";
import { useNutritionEntries, useDeleteNutritionEntry, useNutritionSummary } from "../../hooks/useNutrition.js";
import { NutritionEntryForm } from "../../components/forms/NutritionEntryForm.js";
import { Button } from "../../components/ui/Button.js";
import { Card, CardTitle } from "../../components/ui/Card.js";

function startOfDayIso(): string {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d.toISOString();
}

function endOfDayIso(): string {
  const d = new Date();
  d.setHours(23, 59, 59, 999);
  return d.toISOString();
}

export function FuelTab() {
  const { data: summary, isLoading: summaryLoading } = useNutritionSummary();
  const { data: entries, isLoading: entriesLoading } = useNutritionEntries({
    from: startOfDayIso(),
    to: endOfDayIso(),
  });
  const deleteMutation = useDeleteNutritionEntry();
  const [showForm, setShowForm] = useState(false);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex justify-end">
        <Button size="sm" onClick={() => setShowForm(true)}>
          Log meal
        </Button>
      </div>

      {!summaryLoading && summary && (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
          <Card>
            <CardTitle>Calories consumed</CardTitle>
            <div className="mt-1 text-2xl font-semibold">{summary.totalCalories}</div>
            {summary.targetCalories != null && (
              <div className="text-xs text-ink-muted">of {summary.targetCalories} target</div>
            )}
          </Card>
          <Card>
            <CardTitle>Protein</CardTitle>
            <div className="mt-1 text-2xl font-semibold">{summary.totalProteinG}g</div>
            {summary.targetProteinG != null && (
              <div className="text-xs text-ink-muted">of {summary.targetProteinG}g target</div>
            )}
          </Card>
          <Card>
            <CardTitle>Estimated burn</CardTitle>
            <div className="mt-1 text-2xl font-semibold">{summary.estimatedBurn.totalKcal}</div>
            <div className="text-xs text-ink-muted">estimate — baseline + training</div>
          </Card>
          <Card>
            <CardTitle>Energy balance</CardTitle>
            <div className="mt-1 text-2xl font-semibold">
              {summary.balanceKcal != null ? `${summary.balanceKcal > 0 ? "+" : ""}${summary.balanceKcal}` : "—"}
            </div>
            <div className="text-xs text-ink-muted">
              {summary.balanceKcal == null
                ? "Set a daily burn baseline in Settings"
                : "estimate — consumed minus burned"}
            </div>
          </Card>
        </div>
      )}

      <Card>
        <CardTitle>Today's meals</CardTitle>
        <div className="mt-3 flex flex-col divide-y divide-hairline">
          {!entriesLoading && entries?.length === 0 && (
            <p className="py-6 text-center text-sm text-ink-muted">No meals logged today yet.</p>
          )}
          {entries?.map((entry) => (
            <div key={entry.id} className="flex items-center justify-between py-2.5 text-sm">
              <div>
                <div className="font-medium text-ink-primary">{entry.mealName || "Meal"}</div>
                <div className="text-xs text-ink-secondary">
                  {entry.calories} kcal
                  {entry.proteinG != null && ` · ${entry.proteinG}g protein`}
                  {entry.carbsG != null && ` · ${entry.carbsG}g carbs`}
                  {entry.fatG != null && ` · ${entry.fatG}g fat`}
                </div>
              </div>
              <button
                className="text-xs text-ink-muted hover:text-status-critical"
                onClick={() => deleteMutation.mutate(entry.id)}
              >
                Remove
              </button>
            </div>
          ))}
        </div>
      </Card>

      {showForm && <NutritionEntryForm onClose={() => setShowForm(false)} />}
    </div>
  );
}
