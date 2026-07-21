import type { EnergyBalanceGranularity, EnergyBalancePointDto } from "@momentum/shared";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

function formatPeriodLabel(period: string, granularity: EnergyBalanceGranularity): string {
  if (granularity === "year") return period;
  if (granularity === "month") {
    const [year, month] = period.split("-").map(Number);
    return new Date(year, month - 1, 1).toLocaleDateString(undefined, { month: "short", year: "2-digit" });
  }
  // day or week -- period is an ISO date (the day itself, or that week's Monday)
  return new Date(period).toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

function CustomTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null;
  const row: EnergyBalancePointDto = payload[0]?.payload;
  if (!row) return null;
  return (
    <div className="rounded-md border border-hairline bg-surface px-3 py-2 text-xs shadow-sm">
      <div className="text-ink-secondary">{label}</div>
      <div className="mt-1">
        <span className="font-semibold text-ink-primary">{row.totalCalories}</span>{" "}
        <span className="text-ink-secondary">kcal consumed</span>
      </div>
      <div>
        <span className="font-semibold text-ink-primary">{row.totalBurnKcal}</span>{" "}
        <span className="text-ink-secondary">kcal burned (estimate)</span>
      </div>
      {row.balanceKcal != null && (
        <div className="mt-1 font-medium" style={{ color: row.balanceKcal > 0 ? "var(--series-2)" : "var(--series-1)" }}>
          {row.balanceKcal > 0 ? "+" : ""}
          {row.balanceKcal} balance
        </div>
      )}
    </div>
  );
}

export function EnergyBalanceChart({
  points,
  granularity,
}: {
  points: EnergyBalancePointDto[];
  granularity: EnergyBalanceGranularity;
}) {
  const data = points.map((p) => ({ ...p, label: formatPeriodLabel(p.period, granularity) }));

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: -16, bottom: 0 }} barGap={2}>
        <CartesianGrid stroke="var(--hairline)" vertical={false} />
        <XAxis
          dataKey="label"
          tick={{ fontSize: 11, fill: "var(--text-muted)" }}
          axisLine={{ stroke: "var(--baseline)" }}
          tickLine={false}
          interval="preserveStartEnd"
        />
        <YAxis tick={{ fontSize: 11, fill: "var(--text-muted)" }} axisLine={false} tickLine={false} width={40} />
        <Tooltip content={<CustomTooltip />} />
        <Bar dataKey="totalCalories" fill="var(--series-2)" radius={[4, 4, 0, 0]} maxBarSize={24} />
        <Bar dataKey="totalBurnKcal" fill="var(--series-1)" radius={[4, 4, 0, 0]} maxBarSize={24} />
      </BarChart>
    </ResponsiveContainer>
  );
}
