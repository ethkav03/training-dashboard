import { useState, type ReactNode } from "react";
import { Card } from "../ui/Card.js";

interface Column<T> {
  key: string;
  label: string;
  render: (row: T) => ReactNode;
}

interface ChartCardProps<T> {
  title: string;
  subtitle?: string;
  legend?: { color: string; label: string }[];
  chart: ReactNode;
  tableRows: T[];
  tableColumns: Column<T>[];
  emptyMessage?: string;
}

export function ChartCard<T extends { id?: string }>({
  title,
  subtitle,
  legend,
  chart,
  tableRows,
  tableColumns,
  emptyMessage = "No data logged yet.",
}: ChartCardProps<T>) {
  const [showTable, setShowTable] = useState(false);

  return (
    <Card>
      <div className="flex items-start justify-between gap-2">
        <div>
          <h3 className="text-sm font-medium text-ink-primary">{title}</h3>
          {subtitle && <p className="text-xs text-ink-secondary">{subtitle}</p>}
        </div>
        {tableRows.length > 0 && (
          <button
            className="shrink-0 rounded-md border border-hairline px-2 py-1 text-xs text-ink-secondary hover:text-ink-primary"
            onClick={() => setShowTable((s) => !s)}
          >
            {showTable ? "Chart view" : "Table view"}
          </button>
        )}
      </div>

      {legend && legend.length > 1 && (
        <div className="mt-2 flex flex-wrap gap-3">
          {legend.map((item) => (
            <span key={item.label} className="flex items-center gap-1.5 text-xs text-ink-secondary">
              <span className="inline-block h-0.5 w-3 rounded" style={{ backgroundColor: item.color }} />
              {item.label}
            </span>
          ))}
        </div>
      )}

      <div className="mt-3">
        {tableRows.length === 0 ? (
          <p className="py-8 text-center text-sm text-ink-muted">{emptyMessage}</p>
        ) : showTable ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-hairline text-ink-secondary">
                  {tableColumns.map((col) => (
                    <th key={col.key} className="py-1.5 pr-4 font-medium">
                      {col.label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="tabular-nums">
                {tableRows.map((row, i) => (
                  <tr key={row.id ?? i} className="border-b border-hairline last:border-0">
                    {tableColumns.map((col) => (
                      <td key={col.key} className="py-1.5 pr-4 text-ink-primary">
                        {col.render(row)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          chart
        )}
      </div>
    </Card>
  );
}
