-- CreateEnum
CREATE TYPE "IntegrationProvider" AS ENUM ('WHOOP', 'HEALTH_CONNECT');

-- CreateEnum
CREATE TYPE "SyncStatus" AS ENUM ('IDLE', 'SYNCING', 'SUCCESS', 'ERROR');

-- AlterEnum
ALTER TYPE "DataSource" ADD VALUE 'HEALTH_CONNECT';

-- AlterTable
ALTER TABLE "RecoveryRecord" ADD COLUMN     "externalId" TEXT;

-- AlterTable
ALTER TABLE "TrainingSession" ADD COLUMN     "avgHeartRate" INTEGER,
ADD COLUMN     "externalId" TEXT;

-- AlterTable
ALTER TABLE "WeightEntry" ADD COLUMN     "externalId" TEXT;

-- CreateTable
CREATE TABLE "IntegrationConnection" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "provider" "IntegrationProvider" NOT NULL,
    "accessTokenCiphertext" TEXT,
    "accessTokenIv" TEXT,
    "accessTokenAuthTag" TEXT,
    "refreshTokenCiphertext" TEXT,
    "refreshTokenIv" TEXT,
    "refreshTokenAuthTag" TEXT,
    "scope" TEXT,
    "tokenExpiresAt" TIMESTAMP(3),
    "connectedAt" TIMESTAMP(3),
    "disconnectedAt" TIMESTAMP(3),
    "lastSyncAt" TIMESTAMP(3),
    "lastSyncStatus" "SyncStatus" NOT NULL DEFAULT 'IDLE',
    "lastSyncError" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "IntegrationConnection_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "IntegrationConnection_userId_provider_key" ON "IntegrationConnection"("userId", "provider");

-- CreateIndex
CREATE UNIQUE INDEX "TrainingSession_userId_source_externalId_key" ON "TrainingSession"("userId", "source", "externalId");

-- CreateIndex
CREATE UNIQUE INDEX "WeightEntry_userId_source_externalId_key" ON "WeightEntry"("userId", "source", "externalId");

-- AddForeignKey
ALTER TABLE "IntegrationConnection" ADD CONSTRAINT "IntegrationConnection_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;
