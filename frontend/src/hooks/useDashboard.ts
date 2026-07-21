import { useQuery } from "@tanstack/react-query";
import { getDashboardToday } from "../api/dashboard.js";

export function useDashboardToday() {
  // "Today" aggregates across every pillar -- rather than relying on every
  // mutation everywhere to remember to invalidate ["dashboard","today"],
  // force a fresh fetch whenever this view is mounted (tab switch, nav back)
  // so it can never show data staler than what the other tabs just wrote.
  return useQuery({ queryKey: ["dashboard", "today"], queryFn: getDashboardToday, refetchOnMount: "always" });
}
