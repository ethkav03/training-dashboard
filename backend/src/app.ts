import cors from "cors";
import express from "express";
import passport from "passport";
import { env } from "./config/env.js";
import "./config/passport.js";
import { errorHandler } from "./middleware/errorHandler.js";
import { authRouter } from "./routes/auth.routes.js";
import { userRouter } from "./routes/user.routes.js";
import { weightRouter } from "./routes/weight.routes.js";
import { nutritionRouter } from "./routes/nutrition.routes.js";
import { trainingRouter } from "./routes/training.routes.js";
import { recoveryRouter } from "./routes/recovery.routes.js";
import { goalRouter } from "./routes/goal.routes.js";
import { insightsRouter } from "./routes/insights.routes.js";
import { timelineRouter } from "./routes/timeline.routes.js";
import { achievementRouter } from "./routes/achievement.routes.js";
import { dashboardRouter } from "./routes/dashboard.routes.js";
import { integrationsRouter } from "./routes/integrations.routes.js";
import { whoopRouter } from "./routes/whoop.routes.js";
import { healthConnectRouter } from "./routes/healthConnect.routes.js";

export const app = express();

app.use(cors({ origin: env.frontendUrl, credentials: true }));
app.use(express.json());
app.use(passport.initialize());

app.get("/api/health", (_req, res) => {
  res.json({ status: "ok", time: new Date().toISOString() });
});

app.use("/api/auth", authRouter);
app.use("/api/users", userRouter);
app.use("/api/weight", weightRouter);
app.use("/api/nutrition", nutritionRouter);
app.use("/api/training", trainingRouter);
app.use("/api/recovery", recoveryRouter);
app.use("/api/goals", goalRouter);
app.use("/api/insights", insightsRouter);
app.use("/api/timeline", timelineRouter);
app.use("/api/achievements", achievementRouter);
app.use("/api/dashboard", dashboardRouter);
app.use("/api/integrations", integrationsRouter);
app.use("/api/integrations/whoop", whoopRouter);
app.use("/api/integrations/health-connect", healthConnectRouter);

app.use(errorHandler);
