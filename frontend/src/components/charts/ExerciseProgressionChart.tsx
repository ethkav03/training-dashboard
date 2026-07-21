import type { ExerciseProgressionPointDto } from "@momentum/shared";
import {
  CartesianGrid,
  Line,
  ComposedChart,
  ResponsiveContainer,
  Scatter,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

function CustomTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null;
  const row = payload[0]?.payload;
  if (!row) return null;
  return (
    <div className="rounded-md border border-hairline bg-surface px-3 py-2 text-xs shadow-sm">
      <div className="text-ink-secondary">{new Date(label).toLocaleDateString()}</div>
      <div className="mt-1">
        <span className="font-semibold text-ink-primary">{row.estimated1RM} kg</span>{" "}
        <span className="text-ink-secondary">est. 1RM</span>
      </div>
      <div>
        <span className="font-semibold text-ink-primary">{row.bestWeightKg} kg</span>{" "}
        <span className="text-ink-secondary">best set weight</span>
      </div>
      {row.isPr && <div className="mt-1 font-medium text-status-good">New PR</div>}
    </div>
  );
}

export function ExerciseProgressionChart({ points }: { points: ExerciseProgressionPointDto[] }) {
  return (
    <ResponsiveContainer width="100%" height={280}>
      <ComposedChart data={points} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
        <CartesianGrid stroke="var(--hairline)" vertical={false} />
        <XAxis
          dataKey="date"
          tickFormatter={(d) => new Date(d).toLocaleDateString(undefined, { month: "short", day: "numeric" })}
          tick={{ fontSize: 11, fill: "var(--text-muted)" }}
          axisLine={{ stroke: "var(--baseline)" }}
          tickLine={false}
        />
        <YAxis tick={{ fontSize: 11, fill: "var(--text-muted)" }} axisLine={false} tickLine={false} width={40} />
        <Tooltip content={<CustomTooltip />} />
        <Line type="monotone" dataKey="estimated1RM" stroke="var(--series-1)" strokeWidth={2} dot={false} />
        <Scatter
          dataKey="bestWeightKg"
          fill="var(--text-secondary)"
          fillOpacity={0.35}
          shape={(props: any) =>
            props.payload.isPr ? (
              <circle cx={props.cx} cy={props.cy} r={5} fill="var(--status-good)" />
            ) : (
              <circle cx={props.cx} cy={props.cy} r={3} fill="var(--text-secondary)" fillOpacity={0.35} />
            )
          }
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}
