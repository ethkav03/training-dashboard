import { Router } from "express";
import { requireAuth } from "../middleware/requireAuth.js";
import { prisma } from "../lib/prisma.js";
import type { AchievementDto } from "@momentum/shared";

export const achievementRouter = Router();

achievementRouter.use(requireAuth);

function toAchievementDto(a: {
  id: string;
  type: string;
  title: string;
  description: string | null;
  value: number | null;
  achievedAt: Date;
}): AchievementDto {
  return {
    id: a.id,
    type: a.type as AchievementDto["type"],
    title: a.title,
    description: a.description,
    value: a.value,
    achievedAt: a.achievedAt.toISOString(),
  };
}

achievementRouter.get("/", async (req, res, next) => {
  try {
    const { type } = req.query as { type?: string };
    const achievements = await prisma.achievement.findMany({
      where: { userId: req.userId!, type: type as never },
      orderBy: { achievedAt: "desc" },
    });
    res.json(achievements.map(toAchievementDto));
  } catch (err) {
    next(err);
  }
});
