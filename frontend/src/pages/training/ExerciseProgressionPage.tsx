import { Link, useParams } from "react-router-dom";
import { useExerciseProgression } from "../../hooks/useTraining.js";
import { ChartCard } from "../../components/charts/ChartCard.js";
import { ExerciseProgressionChart } from "../../components/charts/ExerciseProgressionChart.js";
import { Card, CardTitle } from "../../components/ui/Card.js";

export function ExerciseProgressionPage() {
  const { name = "" } = useParams();
  const { data: points, isLoading } = useExerciseProgression(name);

  const bestWeight = points?.length ? Math.max(...points.map((p) => p.bestWeightKg)) : null;
  const best1RM = points?.length ? Math.max(...points.map((p) => p.estimated1RM)) : null;
  const sessionCount = points?.length ?? 0;

  return (
    <div className="flex flex-col gap-4">
      <div>
        <Link to="/training" className="text-xs text-ink-muted hover:text-ink-primary">
          ← Training
        </Link>
        <h1 className="text-xl font-semibold">{decodeURIComponent(name)}</h1>
      </div>

      {!isLoading && points && (
        <div className="grid grid-cols-3 gap-3">
          <Card>
            <CardTitle>Best weight</CardTitle>
            <div className="mt-1 text-2xl font-semibold">{bestWeight != null ? `${bestWeight} kg` : "—"}</div>
          </Card>
          <Card>
            <CardTitle>Best est. 1RM</CardTitle>
            <div className="mt-1 text-2xl font-semibold">{best1RM != null ? `${best1RM} kg` : "—"}</div>
          </Card>
          <Card>
            <CardTitle>Sessions logged</CardTitle>
            <div className="mt-1 text-2xl font-semibold">{sessionCount}</div>
          </Card>
        </div>
      )}

      {points && (
        <ChartCard
          title="Progression"
          subtitle="Estimated 1RM (line) and best set weight per session (dots — filled green on a new PR)"
          chart={<ExerciseProgressionChart points={points} />}
          tableRows={points.map((p, i) => ({ ...p, id: String(i) }))}
          tableColumns={[
            { key: "date", label: "Date", render: (r) => new Date(r.date).toLocaleDateString() },
            { key: "bestWeightKg", label: "Best weight (kg)", render: (r) => r.bestWeightKg },
            { key: "estimated1RM", label: "Est. 1RM (kg)", render: (r) => r.estimated1RM },
            { key: "volume", label: "Volume (kg)", render: (r) => r.volume },
            { key: "isPr", label: "PR", render: (r) => (r.isPr ? "Yes" : "") },
          ]}
        />
      )}
    </div>
  );
}
