import { Card } from "../components/ui/Card.js";

export function TrainingPage() {
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold">Training</h1>
      <Card>
        <p className="text-sm text-ink-secondary">
          Gym, sport, match and custom session logging arrives here in the Training sprint.
        </p>
      </Card>
    </div>
  );
}
