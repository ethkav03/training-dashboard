import { Card } from "../components/ui/Card.js";

export function InsightsPage() {
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold">Insights</h1>
      <Card>
        <p className="text-sm text-ink-secondary">
          Trend-based, self-explaining insights arrive here in the Insights sprint.
        </p>
      </Card>
    </div>
  );
}
