import type { ReadinessLevel } from "@momentum/shared";

const LEVEL_STYLES: Record<ReadinessLevel, { color: string; label: string }> = {
  HIGH: { color: "var(--status-good)", label: "High readiness" },
  MODERATE: { color: "var(--status-warning)", label: "Moderate readiness" },
  LOW: { color: "var(--status-critical)", label: "Low readiness" },
};

export function ReadinessBadge({ level }: { level: ReadinessLevel }) {
  const style = LEVEL_STYLES[level];
  return (
    <span className="inline-flex items-center gap-1.5 text-sm font-medium" style={{ color: style.color }}>
      <span className="inline-block h-2.5 w-2.5 rounded-full" style={{ backgroundColor: style.color }} />
      {style.label}
    </span>
  );
}
