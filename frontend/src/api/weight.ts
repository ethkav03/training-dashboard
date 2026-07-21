import type { WeightEntryDto, WeightTrendDto } from "@momentum/shared";
import { apiClient } from "./client.js";

export async function getWeightTrend(params?: { from?: string; to?: string }): Promise<WeightTrendDto> {
  const { data } = await apiClient.get("/weight/trend", { params });
  return data;
}

export async function createWeightEntry(payload: {
  date: string;
  weightKg: number;
  note?: string;
}): Promise<WeightEntryDto> {
  const { data } = await apiClient.post("/weight", payload);
  return data;
}

export async function updateWeightEntry(
  id: string,
  payload: { date?: string; weightKg?: number; note?: string }
): Promise<WeightEntryDto> {
  const { data } = await apiClient.patch(`/weight/${id}`, payload);
  return data;
}

export async function deleteWeightEntry(id: string): Promise<void> {
  await apiClient.delete(`/weight/${id}`);
}
