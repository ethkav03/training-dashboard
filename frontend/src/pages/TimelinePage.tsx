import { Card } from "../components/ui/Card.js";

export function TimelinePage() {
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold">Timeline</h1>
      <Card>
        <p className="text-sm text-ink-secondary">
          The unified day/week/month timeline arrives here in the Progress sprint.
        </p>
      </Card>
    </div>
  );
}
