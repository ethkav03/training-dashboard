import { useQuery } from "@tanstack/react-query";
import { getDashboardToday } from "../api/dashboard.js";

export function useDashboardToday() {
  return useQuery({ queryKey: ["dashboard", "today"], queryFn: getDashboardToday });
}
