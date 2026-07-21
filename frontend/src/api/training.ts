import type {
  ExerciseProgressionPointDto,
  TrainingSessionDto,
  TrainingSessionWriteDto,
} from "@momentum/shared";
import { apiClient } from "./client.js";

export async function getTrainingSessions(params?: { from?: string; to?: string; type?: string }): Promise<TrainingSessionDto[]> {
  const { data } = await apiClient.get("/training", { params });
  return data;
}

export async function getTrainingSession(id: string): Promise<TrainingSessionDto> {
  const { data } = await apiClient.get(`/training/${id}`);
  return data;
}

export async function createTrainingSession(payload: TrainingSessionWriteDto): Promise<TrainingSessionDto> {
  const { data } = await apiClient.post("/training", payload);
  return data;
}

export async function updateTrainingSession(id: string, payload: TrainingSessionWriteDto): Promise<TrainingSessionDto> {
  const { data } = await apiClient.put(`/training/${id}`, payload);
  return data;
}

export async function deleteTrainingSession(id: string): Promise<void> {
  await apiClient.delete(`/training/${id}`);
}

export async function getExerciseNames(): Promise<string[]> {
  const { data } = await apiClient.get("/training/gym/exercises");
  return data;
}

export async function getExerciseProgression(name: string): Promise<ExerciseProgressionPointDto[]> {
  const { data } = await apiClient.get(`/training/gym/exercises/${encodeURIComponent(name)}/progression`);
  return data;
}

export async function getLoadSummary(): Promise<{ weeklyLoad: number; acwr: number | null; sessionsThisWeek: number }> {
  const { data } = await apiClient.get("/training/load-summary");
  return data;
}
