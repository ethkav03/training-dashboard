import type { IntegrationConnectionDto } from "@momentum/shared";
import { prisma } from "../lib/prisma.js";
import { isWhoopConfigured } from "../config/env.js";

export async function listConnections(userId: string): Promise<IntegrationConnectionDto[]> {
  const rows = await prisma.integrationConnection.findMany({ where: { userId } });
  const whoop = rows.find((r) => r.provider === "WHOOP");
  const healthConnect = rows.find((r) => r.provider === "HEALTH_CONNECT");

  return [
    {
      provider: "WHOOP",
      configured: isWhoopConfigured,
      // "Connected" means we hold a refresh token and haven't been explicitly
      // disconnected -- not whether the access token happens to be expired
      // right now (that's just "needs a refresh on next use").
      connected: Boolean(whoop && !whoop.disconnectedAt && whoop.refreshTokenCiphertext),
      connectedAt: whoop?.connectedAt?.toISOString() ?? null,
      lastSyncAt: whoop?.lastSyncAt?.toISOString() ?? null,
      lastSyncStatus: whoop?.lastSyncStatus ?? "IDLE",
      lastSyncError: whoop?.lastSyncError ?? null,
    },
    {
      provider: "HEALTH_CONNECT",
      // No server-side app registration needed -- always "configured".
      configured: true,
      // Health Connect has no server-side OAuth grant to check; "connected"
      // just means the Android app has synced at least once.
      connected: Boolean(healthConnect?.lastSyncAt),
      connectedAt: healthConnect?.connectedAt?.toISOString() ?? null,
      lastSyncAt: healthConnect?.lastSyncAt?.toISOString() ?? null,
      lastSyncStatus: healthConnect?.lastSyncStatus ?? "IDLE",
      lastSyncError: healthConnect?.lastSyncError ?? null,
    },
  ];
}
