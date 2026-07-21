import { NavLink } from "react-router-dom";
import { clsx } from "clsx";
import { NAV_ITEMS } from "./navItems.js";

export function BottomNav() {
  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-10 flex border-t border-hairline bg-surface pb-[env(safe-area-inset-bottom)]">
      {NAV_ITEMS.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          className={({ isActive }) =>
            clsx(
              "flex-1 py-2.5 text-center text-xs font-medium transition-colors",
              isActive ? "text-series-1" : "text-ink-muted"
            )
          }
        >
          {item.label}
        </NavLink>
      ))}
    </nav>
  );
}
