import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext.js";
import { useDashboardToday } from "../hooks/useDashboard.js";
import { ReadinessBadge } from "../components/cards/ReadinessBadge.js";
import { GoalCard } from "../components/cards/GoalCard.js";
import { InsightCard } from "../components/cards/InsightCard.js";
import { TimelineEntryItem } from "../components/timeline/TimelineEntryItem.js";
import { Card, CardTitle } from "../components/ui/Card.js";
import { Button } from "../components/ui/Button.js";
import { WeightEntryForm } from "../components/forms/WeightEntryForm.js";
import { NutritionEntryForm } from "../components/forms/NutritionEntryForm.js";
import { TrainingSessionForm } from "../components/forms/TrainingSessionForm.js";
import { GoalForm } from "../components/forms/GoalForm.js";
import { RecoveryEntryForm } from "../components/forms/RecoveryEntryForm.js";

type ActiveModal = "weight" | "nutrition" | "training" | "goal" | "recovery" | null;

export function TodayPage() {
  const { user } = useAuth();
  const { data, isLoading } = useDashboardToday();
  const [activeModal, setActiveModal] = useState<ActiveModal>(null);

  const hour = new Date().getHours();
  const greeting = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold">
        {greeting}, {user?.name?.split(" ")[0]}
      </h1>

      {isLoading && <p className="text-sm text-ink-muted">Loading your day...</p>}

      {data && (
        <>
          <Card>
            <CardTitle>Readiness</CardTitle>
            {data.readiness ? (
              <div className="mt-2 flex items-center justify-between gap-4">
                <div className="flex gap-6">
                  <div>
                    <div className="text-xs text-ink-secondary">Recovery</div>
                    <div className="text-3xl font-semibold">{data.readiness.readinessScore}</div>
                    <ReadinessBadge level={data.readiness.readinessLevel} />
                  </div>
                  <div>
                    <div className="text-xs text-ink-secondary">Sleep</div>
                    <div className="text-3xl font-semibold">{data.readiness.sleepScore ?? "—"}</div>
                  </div>
                  <div>
                    <div className="text-xs text-ink-secondary">Yesterday's strain</div>
                    <div className="text-3xl font-semibold">
                      {data.readiness.strain != null ? data.readiness.strain.toFixed(1) : "—"}
                    </div>
                  </div>
                </div>
                <p className="max-w-sm text-right text-sm text-ink-secondary">{data.readiness.recommendation}</p>
              </div>
            ) : (
              <div className="mt-2 flex items-center justify-between">
                <p className="text-sm text-ink-muted">No recovery data logged today yet.</p>
                <Button size="sm" variant="secondary" onClick={() => setActiveModal("recovery")}>
                  Log recovery
                </Button>
              </div>
            )}
          </Card>

          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <Card>
              <CardTitle>Weight</CardTitle>
              <div className="mt-1 text-xl font-semibold">
                {data.weightSummary.latestWeightKg != null ? `${data.weightSummary.latestWeightKg} kg` : "—"}
              </div>
              {data.weightSummary.rateOfChangeKgPerWeek != null && (
                <div className="text-xs text-ink-muted">{data.weightSummary.rateOfChangeKgPerWeek} kg/wk</div>
              )}
            </Card>
            <Card>
              <CardTitle>Calories</CardTitle>
              <div className="mt-1 text-xl font-semibold">{data.nutritionSummary.totalCalories}</div>
              <div className="text-xs text-ink-muted">
                {data.nutritionSummary.targetCalories != null
                  ? `of ${data.nutritionSummary.targetCalories} target`
                  : "consumed today"}
              </div>
            </Card>
            <Card>
              <CardTitle>Training load</CardTitle>
              <div className="mt-1 text-xl font-semibold">{data.trainingLoadSummary.weeklyLoad}</div>
              <div className="text-xs text-ink-muted">{data.trainingLoadSummary.sessionsThisWeek} sessions this week</div>
            </Card>
            <Card>
              <CardTitle>Streak</CardTitle>
              <div className="mt-1 text-xl font-semibold">{data.gamification.loggingStreakDays}d</div>
              <div className="text-xs text-ink-muted">{data.gamification.weeklyCompletionScore}% of last 7 days logged</div>
            </Card>
          </div>

          {data.goalsStrip.length > 0 && (
            <div>
              <div className="mb-2 flex items-center justify-between">
                <h2 className="text-sm font-medium text-ink-secondary">Active goals</h2>
                <Link to="/goals" className="text-xs text-ink-muted hover:text-ink-primary">
                  View all
                </Link>
              </div>
              <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-4">
                {data.goalsStrip.map((goal) => (
                  <GoalCard key={goal.id} goal={goal} />
                ))}
              </div>
            </div>
          )}

          {data.topInsights.length > 0 && (
            <div>
              <div className="mb-2 flex items-center justify-between">
                <h2 className="text-sm font-medium text-ink-secondary">Insights</h2>
                <Link to="/insights" className="text-xs text-ink-muted hover:text-ink-primary">
                  View all
                </Link>
              </div>
              <div className="flex flex-col gap-3">
                {data.topInsights.map((insight) => (
                  <InsightCard key={insight.id} insight={insight} />
                ))}
              </div>
            </div>
          )}

          {data.gamification.recentAchievements.length > 0 && (
            <Card>
              <CardTitle>Recent achievements</CardTitle>
              <div className="mt-2 flex flex-col divide-y divide-hairline">
                {data.gamification.recentAchievements.map((a) => (
                  <div key={a.id} className="flex items-center justify-between py-2 text-sm">
                    <div>
                      <div className="font-medium text-ink-primary">{a.title}</div>
                      {a.description && <div className="text-xs text-ink-secondary">{a.description}</div>}
                    </div>
                    <span className="text-xs text-ink-muted">{new Date(a.achievedAt).toLocaleDateString()}</span>
                  </div>
                ))}
              </div>
            </Card>
          )}

          <Card>
            <div className="flex items-center justify-between">
              <CardTitle>Today's timeline</CardTitle>
              <Link to="/timeline" className="text-xs text-ink-muted hover:text-ink-primary">
                Full timeline
              </Link>
            </div>
            <div className="mt-2 flex flex-col divide-y divide-hairline">
              {data.timelineToday.length === 0 && (
                <p className="py-6 text-center text-sm text-ink-muted">Nothing logged yet today.</p>
              )}
              {data.timelineToday.map((entry) => (
                <TimelineEntryItem key={entry.id} entry={entry} />
              ))}
            </div>
          </Card>

          <Card>
            <CardTitle>Quick actions</CardTitle>
            <div className="mt-3 flex flex-wrap gap-2">
              <Button size="sm" variant="secondary" onClick={() => setActiveModal("weight")}>
                Log weight
              </Button>
              <Button size="sm" variant="secondary" onClick={() => setActiveModal("nutrition")}>
                Log meal
              </Button>
              <Button size="sm" variant="secondary" onClick={() => setActiveModal("training")}>
                Log workout
              </Button>
              <Button size="sm" variant="secondary" onClick={() => setActiveModal("recovery")}>
                Log recovery
              </Button>
              <Button size="sm" variant="secondary" onClick={() => setActiveModal("goal")}>
                New goal
              </Button>
            </div>
          </Card>
        </>
      )}

      {activeModal === "weight" && <WeightEntryForm onClose={() => setActiveModal(null)} />}
      {activeModal === "nutrition" && <NutritionEntryForm onClose={() => setActiveModal(null)} />}
      {activeModal === "training" && <TrainingSessionForm onClose={() => setActiveModal(null)} />}
      {activeModal === "recovery" && <RecoveryEntryForm onClose={() => setActiveModal(null)} />}
      {activeModal === "goal" && <GoalForm onClose={() => setActiveModal(null)} />}
    </div>
  );
}
