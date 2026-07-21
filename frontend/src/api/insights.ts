import type { InsightDto } from "@momentum/shared";
import { apiClient } from "./client.js";

export async function getInsights(): Promise<InsightDto[]> {
  const { data } = await apiClient.get("/insights");
  return data;
}
