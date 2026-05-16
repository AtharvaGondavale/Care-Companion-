-- CreateEnum
CREATE TYPE "Role" AS ENUM ('GUARDIAN', 'ELDER');

-- CreateEnum
CREATE TYPE "MealTiming" AS ENUM ('BEFORE', 'AFTER');

-- CreateTable
CREATE TABLE "User" (
    "id" TEXT NOT NULL,
    "phoneE164" TEXT NOT NULL,
    "role" "Role" NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "OtpChallenge" (
    "id" TEXT NOT NULL,
    "phoneE164" TEXT NOT NULL,
    "role" "Role" NOT NULL,
    "codeHash" TEXT NOT NULL,
    "expiresAt" TIMESTAMP(3) NOT NULL,
    "consumedAt" TIMESTAMP(3),
    "attempts" INTEGER NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "OtpChallenge_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ElderProfile" (
    "id" TEXT NOT NULL,
    "guardianUserId" TEXT NOT NULL,
    "displayName" TEXT NOT NULL,
    "linkedElderPhoneE164" TEXT,
    "avatarIconKey" TEXT,
    "avatarBgArgb" INTEGER,
    "photoUrl" TEXT,

    CONSTRAINT "ElderProfile_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Contact" (
    "id" TEXT NOT NULL,
    "elderProfileId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "phoneE164" TEXT NOT NULL,
    "photoUrl" TEXT,

    CONSTRAINT "Contact_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Medicine" (
    "id" TEXT NOT NULL,
    "elderProfileId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "dosage" TEXT NOT NULL DEFAULT '',
    "form" TEXT NOT NULL DEFAULT 'Tablet',
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "pillImageUrl" TEXT,
    "packetFrontUrl" TEXT,
    "packetBackUrl" TEXT,

    CONSTRAINT "Medicine_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "MedicineSchedule" (
    "id" TEXT NOT NULL,
    "medicineId" TEXT NOT NULL,
    "mealLabel" TEXT NOT NULL,
    "timeLabel" TEXT NOT NULL,
    "mealTiming" "MealTiming" NOT NULL DEFAULT 'BEFORE',
    "withWater" BOOLEAN NOT NULL DEFAULT false,
    "enabled" BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT "MedicineSchedule_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "User_phoneE164_key" ON "User"("phoneE164");

-- CreateIndex
CREATE INDEX "OtpChallenge_phoneE164_idx" ON "OtpChallenge"("phoneE164");

-- CreateIndex
CREATE INDEX "ElderProfile_guardianUserId_idx" ON "ElderProfile"("guardianUserId");

-- CreateIndex
CREATE INDEX "ElderProfile_linkedElderPhoneE164_idx" ON "ElderProfile"("linkedElderPhoneE164");

-- CreateIndex
CREATE INDEX "Contact_elderProfileId_idx" ON "Contact"("elderProfileId");

-- CreateIndex
CREATE INDEX "Medicine_elderProfileId_idx" ON "Medicine"("elderProfileId");

-- CreateIndex
CREATE INDEX "MedicineSchedule_medicineId_idx" ON "MedicineSchedule"("medicineId");

-- AddForeignKey
ALTER TABLE "ElderProfile" ADD CONSTRAINT "ElderProfile_guardianUserId_fkey" FOREIGN KEY ("guardianUserId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Contact" ADD CONSTRAINT "Contact_elderProfileId_fkey" FOREIGN KEY ("elderProfileId") REFERENCES "ElderProfile"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Medicine" ADD CONSTRAINT "Medicine_elderProfileId_fkey" FOREIGN KEY ("elderProfileId") REFERENCES "ElderProfile"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "MedicineSchedule" ADD CONSTRAINT "MedicineSchedule_medicineId_fkey" FOREIGN KEY ("medicineId") REFERENCES "Medicine"("id") ON DELETE CASCADE ON UPDATE CASCADE;
