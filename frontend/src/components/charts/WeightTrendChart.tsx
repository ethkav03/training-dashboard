import type { WeightTrendDto } from "@momentum/shared";
import {
  CartesianGrid,
  ComposedChart,
  Line,
  ReferenceLine,
  ResponsiveContainer,
  Scatter,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

interface MergedPoint {
  date: string;
  raw: number | null;
  average: number | null;
}

function mergeSeries(trend: WeightTrendDto): MergedPoint[] {
  const byDate = new Map<string, MergedPoint>();
  for (const entry of trend.raw) {
    const key = entry.date.slice(0, 10);
    const existing = byDate.get(key);
    byDate.set(key, { date: key, raw: entry.weightKg, average: existing?.average ?? null });
  }
  for (const point of trend.movingAverage) {
    const existing = byDate.get(point.date);
    byDate.set(point.date, { date: point.date, raw: existing?.raw ?? null, average: point.value });
  }
  return Array.from(byDate.values()).sort((a, b) => a.date.localeCompare(b.date));
}

function CustomTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null;
  const raw = payload.find((p: any) => p.dataKey === "raw")?.value;
  const average = payload.find((p: any) => p.dataKey === "average")?.value;
  return (
    <div className="rounded-md border border-hairline bg-surface px-3 py-2 text-xs shadow-sm">
      <div className="text-ink-secondary">{label}</div>
      {raw != null && (
        <div className="mt-1">
          <span className="font-semibold text-ink-primary">{raw} kg</span>{" "}
          <span className="text-ink-secondary">weigh-in</span>
        </div>
      )}
      {average != null && (
        <div>
          <span className="font-semibold text-ink-primary">{average} kg</span>{" "}
          <span className="text-ink-secondary">7-day average</span>
        </div>
      )}
    </div>
  );
}

export function WeightTrendChart({ trend }: { trend: WeightTrendDto }) {
  const data = mergeSeries(trend);

  return (
    <ResponsiveContainer width="100%" height={280}>
      <ComposedChart data={data} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
        <CartesianGrid stroke="var(--hairline)" vertical={false} />
        <XAxis dataKey="date" tick={{ fontSize: 11, fill: "var(--text-muted)" }} axisLine={{ stroke: "var(--baseline)" }} tickLine={false} />
        <YAxis
          domain={["dataMin - 1", "dataMax + 1"]}
          tick={{ fontSize: 11, fill: "var(--text-muted)" }}
          axisLine={false}
          tickLine={false}
          width={40}
        />
        <Tooltip content={<CustomTooltip />} />
        {trend.goalWeightKg != null && (
          <ReferenceLine
            y={trend.goalWeightKg}
            stroke="var(--series-4)"
            strokeDasharray="4 4"
            strokeOpacity={0.6}
            label={{ value: "Goal", position: "insideTopRight", fill: "var(--text-secondary)", fontSize: 11 }}
          />
        )}
        <Scatter dataKey="raw" fill="var(--text-secondary)" fillOpacity={0.35} r={3} />
        <Line
          type="monotone"
          dataKey="average"
          stroke="var(--series-4)"
          strokeWidth={2}
          dot={false}
          connectNulls
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}
