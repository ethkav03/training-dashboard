import { useInsights } from "../hooks/useInsights.js";
import { InsightCard } from "../components/cards/InsightCard.js";

export function InsightsPage() {
  const { data: insights, isLoading } = useInsights();

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold">Insights</h1>
      <p className="text-sm text-ink-secondary">
        Simple, explainable trends from your own data -- every insight shows the numbers behind it.
      </p>

      {!isLoading && insights?.length === 0 && (
        <p className="py-8 text-center text-sm text-ink-muted">
          Not enough history yet to surface a trend. Keep logging and check back in a couple of weeks.
        </p>
      )}

      <div className="flex flex-col gap-3">
        {insights?.map((insight) => (
          <InsightCard key={insight.id} insight={insight} />
        ))}
      </div>
    </div>
  );
}
