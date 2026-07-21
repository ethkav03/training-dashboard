import { Router } from "express";
import { requireAuth } from "../middleware/requireAuth.js";
import { getAllInsights } from "../services/insightsService.js";

export const insightsRouter = Router();

insightsRouter.use(requireAuth);

insightsRouter.get("/", async (req, res, next) => {
  try {
    res.json(await getAllInsights(req.userId!));
  } catch (err) {
    next(err);
  }
});
