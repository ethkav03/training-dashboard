import { NavLink, Navigate, Route, Routes } from "react-router-dom";
import { clsx } from "clsx";
import { BodyTab } from "./progress/BodyTab.js";
import { FuelTab } from "./progress/FuelTab.js";
import { RecoveryTab } from "./progress/RecoveryTab.js";

const TABS = [
  { to: "body", label: "Body" },
  { to: "fuel", label: "Fuel" },
  { to: "recovery", label: "Recovery" },
];

export function ProgressPage() {
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold">Progress</h1>
      <nav className="flex gap-1 border-b border-hairline">
        {TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            className={({ isActive }) =>
              clsx(
                "px-3 py-2 text-sm font-medium border-b-2 -mb-px transition-colors",
                isActive
                  ? "border-series-1 text-ink-primary"
                  : "border-transparent text-ink-secondary hover:text-ink-primary"
              )
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </nav>
      <Routes>
        <Route index element={<Navigate to="body" replace />} />
        <Route path="body" element={<BodyTab />} />
        <Route path="fuel" element={<FuelTab />} />
        <Route path="recovery" element={<RecoveryTab />} />
      </Routes>
    </div>
  );
}
