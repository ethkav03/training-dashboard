import type { NutritionEntryDto, NutritionSummaryDto } from "@momentum/shared";
import { apiClient } from "./client.js";

export async function getNutritionSummary(date?: string): Promise<NutritionSummaryDto> {
  const { data } = await apiClient.get("/nutrition/summary", { params: date ? { date } : undefined });
  return data;
}

export async function getNutritionEntries(params?: { from?: string; to?: string }): Promise<NutritionEntryDto[]> {
  const { data } = await apiClient.get("/nutrition", { params });
  return data;
}

export async function createNutritionEntry(payload: {
  date: string;
  mealName?: string;
  calories: number;
  proteinG?: number;
  carbsG?: number;
  fatG?: number;
  notes?: string;
}): Promise<NutritionEntryDto> {
  const { data } = await apiClient.post("/nutrition", payload);
  return data;
}

export async function deleteNutritionEntry(id: string): Promise<void> {
  await apiClient.delete(`/nutrition/${id}`);
}
