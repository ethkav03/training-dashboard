import { useMemo, useState } from "react";
import { useTimeline } from "../hooks/useTimeline.js";
import { TimelineEntryItem } from "../components/timeline/TimelineEntryItem.js";
import { Card } from "../components/ui/Card.js";
import { Button } from "../components/ui/Button.js";

function startOfDay(d: Date): Date {
  const copy = new Date(d);
  copy.setHours(0, 0, 0, 0);
  return copy;
}
function endOfDay(d: Date): Date {
  const copy = new Date(d);
  copy.setHours(23, 59, 59, 999);
  return copy;
}

export function TimelinePage() {
  const [weekOffset, setWeekOffset] = useState(0);

  const { from, to, rangeLabel } = useMemo(() => {
    const today = new Date();
    const rangeEnd = endOfDay(new Date(today.getTime() + weekOffset * 7 * 24 * 60 * 60 * 1000));
    const rangeStart = startOfDay(new Date(rangeEnd.getTime() - 6 * 24 * 60 * 60 * 1000));
    return {
      from: rangeStart.toISOString(),
      to: rangeEnd.toISOString(),
      rangeLabel: `${rangeStart.toLocaleDateString(undefined, { month: "short", day: "numeric" })} – ${rangeEnd.toLocaleDateString(undefined, { month: "short", day: "numeric" })}`,
    };
  }, [weekOffset]);

  const { data: entries, isLoading } = useTimeline(from, to);

  const groupedByDay = useMemo(() => {
    const groups = new Map<string, typeof entries>();
    for (const entry of entries ?? []) {
      const key = entry.date.slice(0, 10);
      groups.set(key, [...(groups.get(key) ?? []), entry]);
    }
    return Array.from(groups.entries()).sort((a, b) => b[0].localeCompare(a[0]));
  }, [entries]);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Timeline</h1>
        <div className="flex items-center gap-2">
          <Button size="sm" variant="secondary" onClick={() => setWeekOffset((w) => w - 1)}>
            ← Previous week
          </Button>
          <span className="text-sm text-ink-secondary">{rangeLabel}</span>
          <Button size="sm" variant="secondary" onClick={() => setWeekOffset((w) => w + 1)}>
            Next week →
          </Button>
          {weekOffset !== 0 && (
            <Button size="sm" variant="ghost" onClick={() => setWeekOffset(0)}>
              Today
            </Button>
          )}
        </div>
      </div>

      {!isLoading && groupedByDay.length === 0 && (
        <p className="py-8 text-center text-sm text-ink-muted">Nothing logged in this range.</p>
      )}

      {groupedByDay.map(([day, dayEntries]) => (
        <Card key={day}>
          <h2 className="text-sm font-medium text-ink-secondary">
            {new Date(day).toLocaleDateString(undefined, { weekday: "long", month: "short", day: "numeric" })}
          </h2>
          <div className="mt-1 flex flex-col divide-y divide-hairline">
            {dayEntries!.map((entry) => (
              <TimelineEntryItem key={entry.id} entry={entry} />
            ))}
          </div>
        </Card>
      ))}
    </div>
  );
}
