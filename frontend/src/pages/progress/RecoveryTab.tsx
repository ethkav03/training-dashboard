import { useState } from "react";
import { useRecoveryHistory, useRecoveryToday } from "../../hooks/useRecovery.js";
import { RecoveryEntryForm } from "../../components/forms/RecoveryEntryForm.js";
import { ReadinessBadge } from "../../components/cards/ReadinessBadge.js";
import { Button } from "../../components/ui/Button.js";
import { Card, CardTitle } from "../../components/ui/Card.js";

export function RecoveryTab() {
  const { data: today, isLoading } = useRecoveryToday();
  const { data: history } = useRecoveryHistory();
  const [showForm, setShowForm] = useState(false);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex justify-end">
        <Button size="sm" onClick={() => setShowForm(true)}>
          {today ? "Update today's recovery" : "Log recovery"}
        </Button>
      </div>

      <Card>
        <CardTitle>Today's readiness</CardTitle>
        {!isLoading && !today && (
          <p className="mt-2 text-sm text-ink-muted">No recovery data logged today yet.</p>
        )}
        {today && (
          <>
            <div className="mt-2 grid grid-cols-3 gap-3">
              <div>
                <div className="text-xs text-ink-secondary">Recovery</div>
                <div className="text-2xl font-semibold">{today.readinessScore}</div>
                <ReadinessBadge level={today.readinessLevel} />
              </div>
              <div>
                <div className="text-xs text-ink-secondary">Sleep</div>
                <div className="text-2xl font-semibold">{today.sleepScore ?? "—"}</div>
                {today.sleepHours != null && <div className="text-xs text-ink-muted">{today.sleepHours}h</div>}
              </div>
              <div>
                <div className="text-xs text-ink-secondary">Yesterday's strain</div>
                <div className="text-2xl font-semibold">{today.strain != null ? today.strain.toFixed(1) : "—"}</div>
              </div>
            </div>
            <p className="mt-3 text-sm text-ink-secondary">{today.recommendation}</p>
          </>
        )}
      </Card>

      <Card>
        <CardTitle>History</CardTitle>
        <div className="mt-3 flex flex-col divide-y divide-hairline">
          {history?.length === 0 && <p className="py-6 text-center text-sm text-ink-muted">No history yet.</p>}
          {history?.map((r) => (
            <div key={r.id} className="flex items-center justify-between py-2.5 text-sm">
              <div>
                <div className="font-medium text-ink-primary">{new Date(r.date).toLocaleDateString()}</div>
                <div className="text-xs text-ink-secondary">
                  {r.sleepHours != null && `${r.sleepHours}h sleep`}
                  {r.sleepQuality != null && ` · quality ${r.sleepQuality}/5`}
                  {r.soreness != null && ` · soreness ${r.soreness}/5`}
                </div>
              </div>
              <div className="flex items-center gap-4 text-right">
                <div>
                  <div className="text-xs text-ink-muted">Sleep</div>
                  <div className="font-medium">{r.sleepScore ?? "—"}</div>
                </div>
                <div>
                  <div className="text-xs text-ink-muted">Strain</div>
                  <div className="font-medium">{r.strain != null ? r.strain.toFixed(1) : "—"}</div>
                </div>
                <div>
                  <div className="font-semibold">{r.readinessScore}</div>
                  <ReadinessBadge level={r.readinessLevel} />
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {showForm && <RecoveryEntryForm onClose={() => setShowForm(false)} />}
    </div>
  );
}
