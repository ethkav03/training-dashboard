import { Router } from "express";
import { z } from "zod";
import { requireAuth } from "../middleware/requireAuth.js";
import { validateQuery } from "../middleware/validate.js";
import { getTimeline } from "../services/timelineService.js";

export const timelineRouter = Router();

timelineRouter.use(requireAuth);

const timelineQuerySchema = z.object({
  from: z.string().datetime(),
  to: z.string().datetime(),
});

timelineRouter.get("/", validateQuery(timelineQuerySchema), async (req, res, next) => {
  try {
    const { from, to } = req.query as { from: string; to: string };
    res.json(await getTimeline(req.userId!, new Date(from), new Date(to)));
  } catch (err) {
    next(err);
  }
});
