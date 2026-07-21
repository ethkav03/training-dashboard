import type { TimelineEntryDto } from "@momentum/shared";

const KIND_STYLES: Record<TimelineEntryDto["kind"], { color: string; label: string }> = {
  WEIGHT: { color: "var(--series-4)", label: "Body" },
  MEAL: { color: "var(--series-2)", label: "Fuel" },
  TRAINING: { color: "var(--series-1)", label: "Training" },
  RECOVERY: { color: "var(--series-3)", label: "Recovery" },
  ACHIEVEMENT: { color: "var(--series-5)", label: "Achievement" },
};

export function TimelineEntryItem({ entry }: { entry: TimelineEntryDto }) {
  const style = KIND_STYLES[entry.kind];
  return (
    <div className="flex items-start gap-3 py-2">
      <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full" style={{ backgroundColor: style.color }} />
      <div className="flex-1">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium text-ink-primary">{entry.title}</span>
          <span className="text-xs text-ink-muted">
            {new Date(entry.date).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
          </span>
        </div>
        {entry.subtitle && <p className="text-xs text-ink-secondary">{entry.subtitle}</p>}
      </div>
    </div>
  );
}
