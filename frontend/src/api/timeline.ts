import type { TimelineEntryDto } from "@momentum/shared";
import { apiClient } from "./client.js";

export async function getTimeline(from: string, to: string): Promise<TimelineEntryDto[]> {
  const { data } = await apiClient.get("/timeline", { params: { from, to } });
  return data;
}
