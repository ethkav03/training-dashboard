import type { InsightDto } from "@momentum/shared";
import { Card } from "../ui/Card.js";

const TREND_ARROW: Record<InsightDto["trend"], string> = { up: "↑", down: "↓", flat: "→" };

export function InsightCard({ insight }: { insight: InsightDto }) {
  return (
    <Card>
      <div className="flex items-start gap-2">
        <span className="mt-0.5 text-ink-muted">{TREND_ARROW[insight.trend]}</span>
        <div>
          <p className="text-sm font-medium text-ink-primary">{insight.headline}</p>
          <p className="mt-1 text-xs text-ink-secondary">{insight.detail}</p>
        </div>
      </div>
    </Card>
  );
}
