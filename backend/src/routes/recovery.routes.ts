import { Router } from "express";
import { requireAuth } from "../middleware/requireAuth.js";
import { validateBody, validateQuery } from "../middleware/validate.js";
import { upsertRecoveryRecordSchema } from "../validation/recovery.validation.js";
import { dateRangeQuerySchema } from "../validation/weight.validation.js";
import { getRecoveryToday, listRecoveryRecords, toRecoveryRecordDto, upsertRecoveryRecord } from "../services/recoveryService.js";
import { prisma } from "../lib/prisma.js";
import { ApiError } from "../middleware/errorHandler.js";

export const recoveryRouter = Router();

recoveryRouter.use(requireAuth);

recoveryRouter.get("/today", async (req, res, next) => {
  try {
    const record = await getRecoveryToday(req.userId!);
    res.json(record ? toRecoveryRecordDto(record) : null);
  } catch (err) {
    next(err);
  }
});

recoveryRouter.get("/", validateQuery(dateRangeQuerySchema), async (req, res, next) => {
  try {
    const { from, to } = req.query as { from?: string; to?: string };
    const records = await listRecoveryRecords(req.userId!, from ? new Date(from) : undefined, to ? new Date(to) : undefined);
    res.json(records.map(toRecoveryRecordDto));
  } catch (err) {
    next(err);
  }
});

recoveryRouter.post("/", validateBody(upsertRecoveryRecordSchema), async (req, res, next) => {
  try {
    const record = await upsertRecoveryRecord(req.userId!, req.body.date ? new Date(req.body.date) : new Date(), req.body);
    res.status(201).json(toRecoveryRecordDto(record));
  } catch (err) {
    next(err);
  }
});

recoveryRouter.delete("/:id", async (req, res, next) => {
  try {
    const existing = await prisma.recoveryRecord.findFirst({ where: { id: req.params.id, userId: req.userId! } });
    if (!existing) return next(new ApiError(404, "RecoveryRecordNotFound"));
    await prisma.recoveryRecord.delete({ where: { id: existing.id } });
    res.status(204).end();
  } catch (err) {
    next(err);
  }
});
