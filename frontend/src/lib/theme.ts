export type ThemePreference = "light" | "dark" | "system";

const STORAGE_KEY = "momentum.theme";

export function getStoredThemePreference(): ThemePreference {
  const stored = localStorage.getItem(STORAGE_KEY);
  return stored === "light" || stored === "dark" ? stored : "system";
}

export function applyThemePreference(preference: ThemePreference): void {
  const root = document.documentElement;
  if (preference === "system") {
    root.removeAttribute("data-theme");
    localStorage.removeItem(STORAGE_KEY);
  } else {
    root.setAttribute("data-theme", preference);
    localStorage.setItem(STORAGE_KEY, preference);
  }
}
