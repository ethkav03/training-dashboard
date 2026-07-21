export interface NavItem {
  to: string;
  label: string;
}

export const NAV_ITEMS: NavItem[] = [
  { to: "/today", label: "Today" },
  { to: "/progress", label: "Progress" },
  { to: "/training", label: "Training" },
  { to: "/goals", label: "Goals" },
  { to: "/insights", label: "Insights" },
  { to: "/settings", label: "Settings" },
];
