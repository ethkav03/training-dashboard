import { Router } from "express";
import { requireAuth } from "../middleware/requireAuth.js";
import { validateBody } from "../middleware/validate.js";
import { healthConnectSyncSchema } from "../validation/healthConnect.validation.js";
import { syncHealthConnectData } from "../services/healthConnectService.js";
import { prisma } from "../lib/prisma.js";
import { ApiError } from "../middleware/errorHandler.js";

export const healthConnectRouter = Router();

healthConnectRouter.use(requireAuth);

healthConnectRouter.post("/sync", validateBody(healthConnectSyncSchema), async (req, res, next) => {
  const userId = req.userId!;

  const existing = await prisma.integrationConnection.findUnique({
    where: { userId_provider: { userId, provider: "HEALTH_CONNECT" } },
  });
  const connection =
    existing ??
    (await prisma.integrationConnection.create({ data: { userId, provider: "HEALTH_CONNECT" } }));

  await prisma.integrationConnection.update({ where: { id: connection.id }, data: { lastSyncStatus: "SYNCING" } });

  try {
    const result = await syncHealthConnectData(userId, req.body);
    await prisma.integrationConnection.update({
      where: { id: connection.id },
      data: {
        connectedAt: connection.connectedAt ?? new Date(),
        lastSyncAt: new Date(),
        lastSyncStatus: "SUCCESS",
        lastSyncError: null,
      },
    });
    res.json({ status: "SUCCESS", syncedAt: new Date().toISOString(), ...result });
  } catch (err) {
    const message = err instanceof Error ? err.message : "Unknown sync error";
    await prisma.integrationConnection.update({
      where: { id: connection.id },
      data: { lastSyncStatus: "ERROR", lastSyncError: message },
    });
    next(err instanceof Error ? new ApiError(500, message) : err);
  }
});
