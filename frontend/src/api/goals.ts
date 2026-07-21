import type { GoalDto } from "@momentum/shared";
import { apiClient } from "./client.js";

export async function getGoals(params?: { status?: string; type?: string }): Promise<GoalDto[]> {
  const { data } = await apiClient.get("/goals", { params });
  return data;
}

export async function createGoal(payload: {
  type: string;
  title: string;
  targetValue?: number;
  targetUnit?: string;
  direction: string;
  targetDate?: string;
  relatedExerciseName?: string;
  notes?: string;
}): Promise<GoalDto> {
  const { data } = await apiClient.post("/goals", payload);
  return data;
}

export async function updateGoal(
  id: string,
  patch: { status?: "ON_TRACK" | "PAUSED"; title?: string; targetValue?: number; targetDate?: string | null }
): Promise<GoalDto> {
  const { data } = await apiClient.patch(`/goals/${id}`, patch);
  return data;
}

export async function deleteGoal(id: string): Promise<void> {
  await apiClient.delete(`/goals/${id}`);
}
