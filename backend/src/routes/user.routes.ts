import { Router } from "express";
import { requireAuth } from "../middleware/requireAuth.js";
import { validateBody } from "../middleware/validate.js";
import { onboardingSchema, updateProfileSchema } from "../validation/user.validation.js";
import { prisma } from "../lib/prisma.js";
import { completeOnboarding, exportUserData, toUserDto } from "../services/userService.js";
import { OnboardingStatus } from "@momentum/shared";
import { ApiError } from "../middleware/errorHandler.js";

export const userRouter = Router();

userRouter.use(requireAuth);

userRouter.get("/me", async (req, res, next) => {
  try {
    const user = await prisma.user.findUniqueOrThrow({ where: { id: req.userId! } });
    res.json(toUserDto(user));
  } catch (err) {
    next(err);
  }
});

userRouter.patch("/me", validateBody(updateProfileSchema), async (req, res, next) => {
  try {
    const user = await prisma.user.update({
      where: { id: req.userId! },
      data: {
        ...req.body,
        dateOfBirth: req.body.dateOfBirth ? new Date(req.body.dateOfBirth) : undefined,
      },
    });
    res.json(toUserDto(user));
  } catch (err) {
    next(err);
  }
});

userRouter.post("/me/onboarding", validateBody(onboardingSchema), async (req, res, next) => {
  try {
    const user = await completeOnboarding(req.userId!, req.body);
    res.json(toUserDto(user));
  } catch (err) {
    next(err);
  }
});

userRouter.post("/me/onboarding/skip", async (req, res, next) => {
  try {
    const user = await prisma.user.update({
      where: { id: req.userId! },
      data: { onboardingStatus: OnboardingStatus.SKIPPED },
    });
    res.json(toUserDto(user));
  } catch (err) {
    next(err);
  }
});

userRouter.get("/me/export", async (req, res, next) => {
  try {
    const data = await exportUserData(req.userId!);
    res.setHeader("Content-Disposition", "attachment; filename=momentum-export.json");
    res.json(data);
  } catch (err) {
    next(err);
  }
});

userRouter.delete("/me", async (req, res, next) => {
  try {
    await prisma.user.delete({ where: { id: req.userId! } });
    res.status(204).end();
  } catch (err) {
    next(err instanceof Error ? new ApiError(500, err.message) : err);
  }
});
