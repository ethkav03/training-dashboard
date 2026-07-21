import type { DashboardTodayDto } from "@momentum/shared";
import { apiClient } from "./client.js";

export async function getDashboardToday(): Promise<DashboardTodayDto> {
  const { data } = await apiClient.get("/dashboard/today");
  return data;
}
