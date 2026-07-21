import { Card } from "../components/ui/Card.js";

export function GoalsPage() {
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold">Goals</h1>
      <Card>
        <p className="text-sm text-ink-secondary">
          Active goals, targets and status tracking arrive here in the Goals sprint.
        </p>
      </Card>
    </div>
  );
}
