-- CreateEnum
CREATE TYPE "MealType" AS ENUM ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACKS');

-- AlterTable
ALTER TABLE "NutritionEntry" ADD COLUMN     "mealType" "MealType" NOT NULL DEFAULT 'SNACKS';

-- AlterTable
ALTER TABLE "RecoveryRecord" ADD COLUMN     "sleepScore" INTEGER,
ADD COLUMN     "strain" DOUBLE PRECISION;
