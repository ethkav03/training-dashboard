import { Router } from "express";
import { requireAuth } from "../middleware/requireAuth.js";
import { validateBody, validateQuery } from "../middleware/validate.js";
import { createTrainingSessionSchema, trainingQuerySchema } from "../validation/training.validation.js";
import { ApiError } from "../middleware/errorHandler.js";
import {
  createTrainingSession,
  findSessionWithDetail,
  getExerciseProgression,
  getLoadTrend,
  getWeeklyLoadSummary,
  listDistinctExerciseNames,
  listTrainingSessions,
  replaceTrainingSession,
  toTrainingSessionDto,
} from "../services/trainingService.js";
import { prisma } from "../lib/prisma.js";

export const trainingRouter = Router();

trainingRouter.use(requireAuth);

// Static sub-paths must be registered before the `/:id` param route.
trainingRouter.get("/gym/exercises", async (req, res, next) => {
  try {
    res.json(await listDistinctExerciseNames(req.userId!));
  } catch (err) {
    next(err);
  }
});

trainingRouter.get("/gym/exercises/:name/progression", async (req, res, next) => {
  try {
    res.json(await getExerciseProgression(req.userId!, req.params.name));
  } catch (err) {
    next(err);
  }
});

trainingRouter.get("/load-trend", validateQuery(trainingQuerySchema), async (req, res, next) => {
  try {
    const { from, to } = req.query as { from?: string; to?: string };
    const now = new Date();
    const defaultFrom = new Date(now.getTime() - 28 * 24 * 60 * 60 * 1000);
    const trend = await getLoadTrend(req.userId!, from ? new Date(from) : defaultFrom, to ? new Date(to) : now);
    res.json(trend);
  } catch (err) {
    next(err);
  }
});

trainingRouter.get("/load-summary", async (req, res, next) => {
  try {
    res.json(await getWeeklyLoadSummary(req.userId!));
  } catch (err) {
    next(err);
  }
});

trainingRouter.get("/", validateQuery(trainingQuerySchema), async (req, res, next) => {
  try {
    const { from, to, type } = req.query as { from?: string; to?: string; type?: string };
    const sessions = await listTrainingSessions(req.userId!, {
      from: from ? new Date(from) : undefined,
      to: to ? new Date(to) : undefined,
      type,
    });
    res.json(sessions.map(toTrainingSessionDto));
  } catch (err) {
    next(err);
  }
});

trainingRouter.get("/:id", async (req, res, next) => {
  try {
    const session = await findSessionWithDetail(req.userId!, req.params.id);
    if (!session) return next(new ApiError(404, "TrainingSessionNotFound"));
    res.json(toTrainingSessionDto(session));
  } catch (err) {
    next(err);
  }
});

trainingRouter.post("/", validateBody(createTrainingSessionSchema), async (req, res, next) => {
  try {
    const session = await createTrainingSession(req.userId!, req.body);
    res.status(201).json(toTrainingSessionDto(session));
  } catch (err) {
    next(err);
  }
});

trainingRouter.put("/:id", validateBody(createTrainingSessionSchema), async (req, res, next) => {
  try {
    const session = await replaceTrainingSession(req.userId!, req.params.id, req.body);
    if (!session) return next(new ApiError(404, "TrainingSessionNotFound"));
    res.json(toTrainingSessionDto(session));
  } catch (err) {
    next(err);
  }
});

trainingRouter.delete("/:id", async (req, res, next) => {
  try {
    const existing = await prisma.trainingSession.findFirst({ where: { id: req.params.id, userId: req.userId! } });
    if (!existing) return next(new ApiError(404, "TrainingSessionNotFound"));
    await prisma.trainingSession.delete({ where: { id: existing.id } });
    res.status(204).end();
  } catch (err) {
    next(err);
  }
});
