import { useAuth } from "../context/AuthContext.js";
import { Card, CardTitle } from "../components/ui/Card.js";

export function TodayPage() {
  const { user } = useAuth();
  const hour = new Date().getHours();
  const greeting = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold">
        {greeting}, {user?.name?.split(" ")[0]}
      </h1>
      <Card>
        <CardTitle>Today</CardTitle>
        <p className="mt-2 text-sm text-ink-secondary">
          Your daily overview (readiness, calories, training load, timeline and quick actions) will
          assemble here once weight, nutrition, training and recovery logging are in place.
        </p>
      </Card>
    </div>
  );
}
