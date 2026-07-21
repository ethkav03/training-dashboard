import { useState } from "react";
import type { WeightEntryDto } from "@momentum/shared";
import { useDeleteWeightEntry, useWeightTrend } from "../../hooks/useWeight.js";
import { ChartCard } from "../../components/charts/ChartCard.js";
import { WeightTrendChart } from "../../components/charts/WeightTrendChart.js";
import { WeightEntryForm } from "../../components/forms/WeightEntryForm.js";
import { Button } from "../../components/ui/Button.js";
import { Card, CardTitle } from "../../components/ui/Card.js";

export function BodyTab() {
  const { data: trend, isLoading } = useWeightTrend();
  const deleteMutation = useDeleteWeightEntry();
  const [showForm, setShowForm] = useState(false);
  const [editingEntry, setEditingEntry] = useState<WeightEntryDto | null>(null);

  const recentEntries = [...(trend?.raw ?? [])].sort((a, b) => b.date.localeCompare(a.date)).slice(0, 10);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex justify-end">
        <Button size="sm" onClick={() => setShowForm(true)}>
          Log weight
        </Button>
      </div>

      {!isLoading && trend && (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
          <Card>
            <CardTitle>Latest</CardTitle>
            <div className="mt-1 text-2xl font-semibold">
              {trend.latestWeightKg != null ? `${trend.latestWeightKg} kg` : "—"}
            </div>
          </Card>
          <Card>
            <CardTitle>7-day average</CardTitle>
            <div className="mt-1 text-2xl font-semibold">
              {trend.movingAverage.at(-1)?.value != null ? `${trend.movingAverage.at(-1)!.value} kg` : "—"}
            </div>
          </Card>
          <Card>
            <CardTitle>Rate of change</CardTitle>
            <div className="mt-1 text-2xl font-semibold">
              {trend.rateOfChangeKgPerWeek != null ? `${trend.rateOfChangeKgPerWeek} kg/wk` : "—"}
            </div>
          </Card>
          <Card>
            <CardTitle>Goal</CardTitle>
            <div className="mt-1 text-2xl font-semibold">
              {trend.goalWeightKg != null ? `${trend.goalWeightKg} kg` : "Not set"}
            </div>
          </Card>
        </div>
      )}

      {trend && (
        <ChartCard
          title="Weight trend"
          subtitle="Raw weigh-ins (muted) with a smoothed 7-day average (bold)"
          legend={[
            { color: "var(--text-secondary)", label: "Raw weigh-in" },
            { color: "var(--series-4)", label: "7-day average" },
          ]}
          chart={<WeightTrendChart trend={trend} />}
          tableRows={trend.raw}
          tableColumns={[
            { key: "date", label: "Date", render: (r) => new Date(r.date).toLocaleDateString() },
            { key: "weightKg", label: "Weight (kg)", render: (r) => r.weightKg },
            { key: "source", label: "Source", render: (r) => r.source },
          ]}
          emptyMessage="No weigh-ins logged yet — log your first weight to start the trend."
        />
      )}

      <Card>
        <CardTitle>Recent weigh-ins</CardTitle>
        <div className="mt-2 flex flex-col divide-y divide-hairline">
          {recentEntries.length === 0 && <p className="py-6 text-center text-sm text-ink-muted">No weigh-ins yet.</p>}
          {recentEntries.map((entry) => (
            <div key={entry.id} className="flex items-center justify-between py-2.5 text-sm">
              <div>
                <div className="font-medium text-ink-primary">{entry.weightKg} kg</div>
                <div className="text-xs text-ink-secondary">
                  {new Date(entry.date).toLocaleDateString()}
                  {entry.note && ` · ${entry.note}`}
                  {entry.source !== "MANUAL" && ` · ${entry.source}`}
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

      {showForm && <WeightEntryForm onClose={() => setShowForm(false)} />}
      {editingEntry && <WeightEntryForm entry={editingEntry} onClose={() => setEditingEntry(null)} />}
    </div>
  );
}
