import { NavLink } from "react-router-dom";
import { clsx } from "clsx";
import { NAV_ITEMS } from "./navItems.js";
import { useAuth } from "../../context/AuthContext.js";

export function TopNav() {
  const { user, logout } = useAuth();

  return (
    <header className="hidden md:flex items-center justify-between border-b border-hairline bg-surface px-6 py-3">
      <div className="flex items-center gap-8">
        <span className="text-lg font-semibold tracking-tight">Momentum</span>
        <nav className="flex items-center gap-1">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                clsx(
                  "rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-page text-ink-primary"
                    : "text-ink-secondary hover:text-ink-primary hover:bg-page"
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>
      <div className="flex items-center gap-3">
        {user?.avatarUrl ? (
          <img src={user.avatarUrl} alt={user.name} className="h-8 w-8 rounded-full" />
        ) : (
          <div className="h-8 w-8 rounded-full bg-series-1 text-white flex items-center justify-center text-xs font-semibold">
            {user?.name?.[0]?.toUpperCase() ?? "?"}
          </div>
        )}
        <span className="text-sm text-ink-secondary">{user?.name}</span>
        <button onClick={logout} className="text-sm text-ink-muted hover:text-ink-primary">
          Sign out
        </button>
      </div>
    </header>
  );
}
