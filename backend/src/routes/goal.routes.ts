import { Router } from "express";
import { requireAuth } from "../middleware/requireAuth.js";
import { validateBody, validateQuery } from "../middleware/validate.js";
import { createGoalSchema, goalQuerySchema, updateGoalSchema } from "../validation/goal.validation.js";
import { createGoal, deleteGoal, getGoal, listGoals, updateGoal } from "../services/goalService.js";
import { ApiError } from "../middleware/errorHandler.js";

export const goalRouter = Router();

goalRouter.use(requireAuth);

goalRouter.get("/", validateQuery(goalQuerySchema), async (req, res, next) => {
  try {
    const { status, type } = req.query as { status?: string; type?: string };
    res.json(await listGoals(req.userId!, { status, type }));
  } catch (err) {
    next(err);
  }
});

goalRouter.get("/:id", async (req, res, next) => {
  try {
    const goal = await getGoal(req.userId!, req.params.id);
    if (!goal) return next(new ApiError(404, "GoalNotFound"));
    res.json(goal);
  } catch (err) {
    next(err);
  }
});

goalRouter.post("/", validateBody(createGoalSchema), async (req, res, next) => {
  try {
    res.status(201).json(await createGoal(req.userId!, req.body));
  } catch (err) {
    next(err);
  }
});

goalRouter.patch("/:id", validateBody(updateGoalSchema), async (req, res, next) => {
  try {
    const goal = await updateGoal(req.userId!, req.params.id, req.body);
    if (!goal) return next(new ApiError(404, "GoalNotFound"));
    res.json(goal);
  } catch (err) {
    next(err);
  }
});

goalRouter.delete("/:id", async (req, res, next) => {
  try {
    const deleted = await deleteGoal(req.userId!, req.params.id);
    if (!deleted) return next(new ApiError(404, "GoalNotFound"));
    res.status(204).end();
  } catch (err) {
    next(err);
  }
});
