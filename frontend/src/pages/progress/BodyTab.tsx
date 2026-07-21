import { useState } from "react";
import { useWeightTrend } from "../../hooks/useWeight.js";
import { ChartCard } from "../../components/charts/ChartCard.js";
import { WeightTrendChart } from "../../components/charts/WeightTrendChart.js";
import { WeightEntryForm } from "../../components/forms/WeightEntryForm.js";
import { Button } from "../../components/ui/Button.js";
import { Card, CardTitle } from "../../components/ui/Card.js";

export function BodyTab() {
  const { data: trend, isLoading } = useWeightTrend();
  const [showForm, setShowForm] = useState(false);

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

      {showForm && <WeightEntryForm onClose={() => setShowForm(false)} />}
    </div>
  );
}
