import { Router } from "express";
import { requireAuth } from "../middleware/requireAuth.js";
import { validateBody, validateQuery } from "../middleware/validate.js";
import {
  createNutritionEntrySchema,
  summaryQuerySchema,
  summaryRangeQuerySchema,
  updateNutritionEntrySchema,
} from "../validation/nutrition.validation.js";
import { dateRangeQuerySchema } from "../validation/weight.validation.js";
import { prisma } from "../lib/prisma.js";
import { getNutritionSummaryForDate, getNutritionSummaryRange, toNutritionEntryDto } from "../services/nutritionService.js";
import { recordStreakMilestonesIfAny } from "../services/gamificationService.js";
import { ApiError } from "../middleware/errorHandler.js";

export const nutritionRouter = Router();

nutritionRouter.use(requireAuth);

nutritionRouter.get("/", validateQuery(dateRangeQuerySchema), async (req, res, next) => {
  try {
    const { from, to } = req.query as { from?: string; to?: string };
    const entries = await prisma.nutritionEntry.findMany({
      where: {
        userId: req.userId!,
        date: { gte: from ? new Date(from) : undefined, lte: to ? new Date(to) : undefined },
      },
      orderBy: { date: "desc" },
    });
    res.json(entries.map(toNutritionEntryDto));
  } catch (err) {
    next(err);
  }
});

nutritionRouter.get("/summary", validateQuery(summaryQuerySchema), async (req, res, next) => {
  try {
    const { date } = req.query as { date?: string };
    const summary = await getNutritionSummaryForDate(req.userId!, date ? new Date(date) : new Date());
    res.json(summary);
  } catch (err) {
    next(err);
  }
});

nutritionRouter.get("/summary/range", validateQuery(summaryRangeQuerySchema), async (req, res, next) => {
  try {
    const { from, to } = req.query as { from: string; to: string };
    const summaries = await getNutritionSummaryRange(req.userId!, new Date(from), new Date(to));
    res.json(summaries);
  } catch (err) {
    next(err);
  }
});

nutritionRouter.post("/", validateBody(createNutritionEntrySchema), async (req, res, next) => {
  try {
    const entry = await prisma.nutritionEntry.create({
      data: { ...req.body, userId: req.userId!, date: new Date(req.body.date), source: "MANUAL" },
    });
    await recordStreakMilestonesIfAny(req.userId!);
    res.status(201).json(toNutritionEntryDto(entry));
  } catch (err) {
    next(err);
  }
});

nutritionRouter.patch("/:id", validateBody(updateNutritionEntrySchema), async (req, res, next) => {
  try {
    const existing = await prisma.nutritionEntry.findFirst({ where: { id: req.params.id, userId: req.userId! } });
    if (!existing) return next(new ApiError(404, "NutritionEntryNotFound"));
    const entry = await prisma.nutritionEntry.update({
      where: { id: existing.id },
      data: { ...req.body, date: req.body.date ? new Date(req.body.date) : undefined, isManuallyEdited: true },
    });
    res.json(toNutritionEntryDto(entry));
  } catch (err) {
    next(err);
  }
});

nutritionRouter.delete("/:id", async (req, res, next) => {
  try {
    const existing = await prisma.nutritionEntry.findFirst({ where: { id: req.params.id, userId: req.userId! } });
    if (!existing) return next(new ApiError(404, "NutritionEntryNotFound"));
    await prisma.nutritionEntry.delete({ where: { id: existing.id } });
    res.status(204).end();
  } catch (err) {
    next(err);
  }
});
