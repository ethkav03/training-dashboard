import type { RecoveryRecordDto } from "@momentum/shared";
import { apiClient } from "./client.js";

export async function getRecoveryToday(): Promise<RecoveryRecordDto | null> {
  const { data } = await apiClient.get("/recovery/today");
  return data;
}

export async function getRecoveryHistory(params?: { from?: string; to?: string }): Promise<RecoveryRecordDto[]> {
  const { data } = await apiClient.get("/recovery", { params });
  return data;
}

export async function logRecovery(payload: {
  sleepHours?: number;
  sleepQuality?: number;
  restingHr?: number;
  hrv?: number;
  soreness?: number;
  energy?: number;
  notes?: string;
}): Promise<RecoveryRecordDto> {
  const { data } = await apiClient.post("/recovery", payload);
  return data;
}
