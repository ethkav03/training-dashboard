import { useState } from "react";
import type { NutritionEntryDto } from "@momentum/shared";
import { EnergyBalanceGranularity } from "@momentum/shared";
import { clsx } from "clsx";
import {
  useEnergyBalanceSeries,
  useNutritionEntries,
  useDeleteNutritionEntry,
  useNutritionSummary,
} from "../../hooks/useNutrition.js";
import { NutritionEntryForm } from "../../components/forms/NutritionEntryForm.js";
import { ChartCard } from "../../components/charts/ChartCard.js";
import { EnergyBalanceChart } from "../../components/charts/EnergyBalanceChart.js";
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

const GRANULARITY_TABS: { value: EnergyBalanceGranularity; label: string }[] = [
  { value: EnergyBalanceGranularity.DAY, label: "Day" },
  { value: EnergyBalanceGranularity.WEEK, label: "Week" },
  { value: EnergyBalanceGranularity.MONTH, label: "Month" },
  { value: EnergyBalanceGranularity.YEAR, label: "Year" },
];

export function FuelTab() {
  const { data: summary, isLoading: summaryLoading } = useNutritionSummary();
  const { data: entries, isLoading: entriesLoading } = useNutritionEntries({
    from: startOfDayIso(),
    to: endOfDayIso(),
  });
  const deleteMutation = useDeleteNutritionEntry();
  const [showForm, setShowForm] = useState(false);
  const [editingEntry, setEditingEntry] = useState<NutritionEntryDto | null>(null);
  const [granularity, setGranularity] = useState<EnergyBalanceGranularity>(EnergyBalanceGranularity.DAY);
  const { data: series } = useEnergyBalanceSeries(granularity);

  const latest = series?.at(-1);
  const consumedVsBurnedMax = latest ? Math.max(latest.totalCalories, latest.totalBurnKcal, 1) : 1;

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

      <div className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-medium text-ink-secondary">Consumed vs. burned</h2>
          <nav className="flex gap-1">
            {GRANULARITY_TABS.map((tab) => (
              <button
                key={tab.value}
                onClick={() => setGranularity(tab.value)}
                className={clsx(
                  "rounded-md px-2.5 py-1 text-xs font-medium transition-colors",
                  granularity === tab.value
                    ? "bg-surface text-ink-primary shadow-sm"
                    : "text-ink-secondary hover:text-ink-primary"
                )}
              >
                {tab.label}
              </button>
            ))}
          </nav>
        </div>

        {latest && (
          <Card>
            <CardTitle>Latest {granularity === "day" ? "day" : granularity}</CardTitle>
            <div className="mt-2 flex flex-col gap-2">
              <div>
                <div className="mb-1 flex items-center justify-between text-xs">
                  <span className="text-ink-secondary">Consumed</span>
                  <span className="font-medium text-ink-primary">{latest.totalCalories} kcal</span>
                </div>
                <div className="h-2 w-full rounded-full bg-page">
                  <div
                    className="h-2 rounded-full bg-series-2"
                    style={{ width: `${(latest.totalCalories / consumedVsBurnedMax) * 100}%` }}
                  />
                </div>
              </div>
              <div>
                <div className="mb-1 flex items-center justify-between text-xs">
                  <span className="text-ink-secondary">Burned (estimate)</span>
                  <span className="font-medium text-ink-primary">{latest.totalBurnKcal} kcal</span>
                </div>
                <div className="h-2 w-full rounded-full bg-page">
                  <div
                    className="h-2 rounded-full bg-series-1"
                    style={{ width: `${(latest.totalBurnKcal / consumedVsBurnedMax) * 100}%` }}
                  />
                </div>
              </div>
            </div>
          </Card>
        )}

        {series && (
          <ChartCard
            title="Trend"
            legend={[
              { color: "var(--series-2)", label: "Consumed" },
              { color: "var(--series-1)", label: "Burned (estimate)" },
            ]}
            chart={<EnergyBalanceChart points={series} granularity={granularity} />}
            tableRows={series.map((p) => ({ ...p, id: p.period }))}
            tableColumns={[
              { key: "period", label: "Period", render: (r) => r.period },
              { key: "totalCalories", label: "Consumed (kcal)", render: (r) => r.totalCalories },
              { key: "totalBurnKcal", label: "Burned (kcal)", render: (r) => r.totalBurnKcal },
              {
                key: "balanceKcal",
                label: "Balance (kcal)",
                render: (r) => (r.balanceKcal != null ? `${r.balanceKcal > 0 ? "+" : ""}${r.balanceKcal}` : "—"),
              },
            ]}
            emptyMessage="Not enough data yet for this view."
          />
        )}
      </div>

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
              <div className="flex items-center gap-3 text-xs">
                <button className="text-ink-muted hover:text-ink-primary" onClick={() => setEditingEntry(entry)}>
                  Edit
                </button>
                <button
                  className="text-ink-muted hover:text-status-critical"
                  onClick={() => deleteMutation.mutate(entry.id)}
                >
                  Remove
                </button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {showForm && <NutritionEntryForm onClose={() => setShowForm(false)} />}
      {editingEntry && <NutritionEntryForm entry={editingEntry} onClose={() => setEditingEntry(null)} />}
    </div>
  );
}
