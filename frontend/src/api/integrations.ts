import type { IntegrationConnectionDto, WhoopSyncResultDto } from "@momentum/shared";
import { apiClient } from "./client.js";

export async function getIntegrations(): Promise<IntegrationConnectionDto[]> {
  const { data } = await apiClient.get("/integrations");
  return data;
}

export async function getWhoopConnectUrl(): Promise<{ authorizeUrl: string }> {
  const { data } = await apiClient.get("/integrations/whoop/connect");
  return data;
}

export async function syncWhoopNow(): Promise<WhoopSyncResultDto> {
  const { data } = await apiClient.post("/integrations/whoop/sync");
  return data;
}

export async function disconnectWhoop(): Promise<void> {
  await apiClient.delete("/integrations/whoop");
}
