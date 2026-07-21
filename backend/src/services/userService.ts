import type { User } from "@prisma/client";
import type { UserDto } from "@momentum/shared";
import { GoalDirection, OnboardingStatus } from "@momentum/shared";
import { prisma } from "../lib/prisma.js";

interface GoogleIdentity {
  googleId: string;
  email: string;
  name: string;
  avatarUrl?: string | null;
}

/** Shared by both the web OAuth callback (Passport) and the mobile ID-token exchange. */
export async function upsertUserFromGoogleIdentity(identity: GoogleIdentity): Promise<User> {
  return prisma.user.upsert({
    where: { googleId: identity.googleId },
    update: {
      email: identity.email,
      name: identity.name,
      avatarUrl: identity.avatarUrl,
    },
    create: {
      googleId: identity.googleId,
      email: identity.email,
      name: identity.name,
      avatarUrl: identity.avatarUrl,
    },
  });
}

export function toUserDto(user: User): UserDto {
  return {
    id: user.id,
    email: user.email,
    name: user.name,
    avatarUrl: user.avatarUrl,
    unitSystem: user.unitSystem as UserDto["unitSystem"],
    dateOfBirth: user.dateOfBirth?.toISOString() ?? null,
    heightCm: user.heightCm,
    estimatedDailyBurnKcal: user.estimatedDailyBurnKcal,
    onboardingStatus: user.onboardingStatus as UserDto["onboardingStatus"],
    createdAt: user.createdAt.toISOString(),
  };
}

interface OnboardingInput {
  dateOfBirth?: string;
  heightCm?: number;
  currentWeightKg?: number;
  unitSystem?: "METRIC" | "IMPERIAL";
  trainingFrequencyPerWeek?: number;
  primaryGoal?: {
    type: string;
    title: string;
    targetValue: number;
    targetUnit: string;
    direction: string;
    targetDate?: string;
  };
}

export async function completeOnboarding(userId: string, input: OnboardingInput): Promise<User> {
  return prisma.$transaction(async (tx) => {
    const user = await tx.user.update({
      where: { id: userId },
      data: {
        dateOfBirth: input.dateOfBirth ? new Date(input.dateOfBirth) : undefined,
        heightCm: input.heightCm,
        unitSystem: input.unitSystem,
        onboardingStatus: OnboardingStatus.COMPLETED,
      },
    });

    if (input.currentWeightKg) {
      await tx.weightEntry.create({
        data: {
          userId,
          date: new Date(),
          weightKg: input.currentWeightKg,
          source: "MANUAL",
        },
      });
    }

    if (input.primaryGoal) {
      const startValue =
        input.primaryGoal.type === "BODY_WEIGHT" ? input.currentWeightKg ?? null : null;
      await tx.goal.create({
        data: {
          userId,
          type: input.primaryGoal.type as never,
          title: input.primaryGoal.title,
          targetValue: input.primaryGoal.targetValue,
          targetUnit: input.primaryGoal.targetUnit,
          direction: input.primaryGoal.direction as GoalDirection,
          startValue,
          startDate: new Date(),
          targetDate: input.primaryGoal.targetDate ? new Date(input.primaryGoal.targetDate) : null,
        },
      });
    }

    if (input.trainingFrequencyPerWeek) {
      await tx.goal.create({
        data: {
          userId,
          type: "TRAINING_FREQUENCY",
          title: "Weekly training frequency",
          targetValue: input.trainingFrequencyPerWeek,
          targetUnit: "sessions/wk",
          direction: "INCREASE",
          startValue: 0,
          startDate: new Date(),
        },
      });
    }

    return user;
  });
}

export async function exportUserData(userId: string) {
  const [user, weightEntries, nutritionEntries, trainingSessions, recoveryRecords, goals, achievements] =
    await Promise.all([
      prisma.user.findUniqueOrThrow({ where: { id: userId } }),
      prisma.weightEntry.findMany({ where: { userId } }),
      prisma.nutritionEntry.findMany({ where: { userId } }),
      prisma.trainingSession.findMany({
        where: { userId },
        include: { workout: { include: { exercises: { include: { sets: true } } } }, matchDetail: true },
      }),
      prisma.recoveryRecord.findMany({ where: { userId } }),
      prisma.goal.findMany({ where: { userId } }),
      prisma.achievement.findMany({ where: { userId } }),
    ]);

  return {
    exportedAt: new Date().toISOString(),
    user,
    weightEntries,
    nutritionEntries,
    trainingSessions,
    recoveryRecords,
    goals,
    achievements,
  };
}
