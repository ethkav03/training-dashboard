import { Router } from "express";
import { requireAuth } from "../middleware/requireAuth.js";
import { validateBody, validateQuery } from "../middleware/validate.js";
import { createWeightEntrySchema, dateRangeQuerySchema, updateWeightEntrySchema } from "../validation/weight.validation.js";
import { prisma } from "../lib/prisma.js";
import { getWeightTrend, toWeightEntryDto } from "../services/weightService.js";
import { recordStreakMilestonesIfAny } from "../services/gamificationService.js";
import { ApiError } from "../middleware/errorHandler.js";

export const weightRouter = Router();

weightRouter.use(requireAuth);

weightRouter.get("/", validateQuery(dateRangeQuerySchema), async (req, res, next) => {
  try {
    const { from, to } = req.query as { from?: string; to?: string };
    const entries = await prisma.weightEntry.findMany({
      where: {
        userId: req.userId!,
        date: { gte: from ? new Date(from) : undefined, lte: to ? new Date(to) : undefined },
      },
      orderBy: { date: "desc" },
    });
    res.json(entries.map(toWeightEntryDto));
  } catch (err) {
    next(err);
  }
});

weightRouter.get("/trend", validateQuery(dateRangeQuerySchema), async (req, res, next) => {
  try {
    const { from, to } = req.query as { from?: string; to?: string };
    const trend = await getWeightTrend(req.userId!, from ? new Date(from) : undefined, to ? new Date(to) : undefined);
    res.json(trend);
  } catch (err) {
    next(err);
  }
});

weightRouter.post("/", validateBody(createWeightEntrySchema), async (req, res, next) => {
  try {
    const entry = await prisma.weightEntry.create({
      data: {
        userId: req.userId!,
        date: new Date(req.body.date),
        weightKg: req.body.weightKg,
        note: req.body.note,
        source: "MANUAL",
      },
    });
    await recordStreakMilestonesIfAny(req.userId!);
    res.status(201).json(toWeightEntryDto(entry));
  } catch (err) {
    next(err);
  }
});

weightRouter.patch("/:id", validateBody(updateWeightEntrySchema), async (req, res, next) => {
  try {
    const existing = await prisma.weightEntry.findFirst({ where: { id: req.params.id, userId: req.userId! } });
    if (!existing) return next(new ApiError(404, "WeightEntryNotFound"));
    const entry = await prisma.weightEntry.update({
      where: { id: existing.id },
      data: {
        date: req.body.date ? new Date(req.body.date) : undefined,
        weightKg: req.body.weightKg,
        note: req.body.note,
        isManuallyEdited: true,
      },
    });
    res.json(toWeightEntryDto(entry));
  } catch (err) {
    next(err);
  }
});

weightRouter.delete("/:id", async (req, res, next) => {
  try {
    const existing = await prisma.weightEntry.findFirst({ where: { id: req.params.id, userId: req.userId! } });
    if (!existing) return next(new ApiError(404, "WeightEntryNotFound"));
    await prisma.weightEntry.delete({ where: { id: existing.id } });
    res.status(204).end();
  } catch (err) {
    next(err);
  }
});
