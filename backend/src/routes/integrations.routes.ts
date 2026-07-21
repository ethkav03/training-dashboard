import { Router } from "express";
import { requireAuth } from "../middleware/requireAuth.js";
import { listConnections } from "../services/integrationService.js";

export const integrationsRouter = Router();

integrationsRouter.use(requireAuth);

integrationsRouter.get("/", async (req, res, next) => {
  try {
    res.json(await listConnections(req.userId!));
  } catch (err) {
    next(err);
  }
});
