import { Router } from "express";
import { requireAuth } from "../middleware/requireAuth.js";
import { getDashboardToday } from "../services/dashboardService.js";

export const dashboardRouter = Router();

dashboardRouter.use(requireAuth);

dashboardRouter.get("/today", async (req, res, next) => {
  try {
    res.json(await getDashboardToday(req.userId!));
  } catch (err) {
    next(err);
  }
});
