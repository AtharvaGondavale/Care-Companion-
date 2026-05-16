import { PrismaClient, Role, MealTiming } from "@prisma/client";
import Fastify from "fastify";
import cors from "@fastify/cors";
import { z } from "zod";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import twilio from "twilio";

const prisma = new PrismaClient();

const JWT_SECRET = process.env.JWT_SECRET ?? "dev-insecure-change-me";
const PORT = Number(process.env.PORT ?? 3000);
const TWILIO_ACCOUNT_SID = process.env.TWILIO_ACCOUNT_SID ?? "";
const TWILIO_AUTH_TOKEN = process.env.TWILIO_AUTH_TOKEN ?? "";
const TWILIO_FROM_NUMBER = process.env.TWILIO_FROM_NUMBER ?? "";
const TWILIO_MOCK = (process.env.TWILIO_MOCK ?? "false").toLowerCase() === "true";

function normalizePhone(input: string): string {
  const digits = input.replace(/\D/g, "");
  if (digits.length >= 10 && digits.length <= 15) return digits;
  throw new Error("Invalid phone");
}

function toE164(digits: string): string {
  if (digits.startsWith("0")) return digits;
  if (digits.length === 10) return `91${digits}`;
  return digits;
}

const requestOtpSchema = z.object({
  phone: z.string().min(8),
  role: z.enum(["GUARDIAN", "ELDER"]),
});

const verifyOtpSchema = z.object({
  phone: z.string().min(8),
  code: z.string().min(4).max(8),
});

function signToken(userId: string, role: Role, phoneE164: string): string {
  return jwt.sign({ sub: userId, role, phoneE164 }, JWT_SECRET, { expiresIn: "30d" });
}

type JwtPayload = { sub: string; role: Role; phoneE164: string };

function verifyBearer(authHeader: string | undefined): JwtPayload {
  if (!authHeader?.startsWith("Bearer ")) throw new Error("Missing token");
  const token = authHeader.slice("Bearer ".length).trim();
  const decoded = jwt.verify(token, JWT_SECRET) as JwtPayload;
  return decoded;
}

async function sendSms(toDigits: string, body: string) {
  const to = toDigits.startsWith("+") ? toDigits : `+${toDigits}`;
  if (TWILIO_MOCK || !TWILIO_ACCOUNT_SID || !TWILIO_AUTH_TOKEN || !TWILIO_FROM_NUMBER) {
    console.log(`[SMS MOCK] to=${to} body=${body}`);
    return;
  }
  const client = twilio(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN);
  await client.messages.create({ from: TWILIO_FROM_NUMBER, to, body });
}

function randomOtp(): string {
  return String(Math.floor(100000 + Math.random() * 900000));
}

export async function buildApp() {
  const app = Fastify({ logger: true });
  await app.register(cors, { origin: true });

  app.get("/health", async () => ({ ok: true }));

  app.post("/v1/auth/otp/request", async (req, reply) => {
    const parsed = requestOtpSchema.safeParse(req.body);
    if (!parsed.success) return reply.status(400).send({ error: "Invalid body" });
    const role = parsed.data.role === "GUARDIAN" ? Role.GUARDIAN : Role.ELDER;
    let phoneE164: string;
    try {
      phoneE164 = toE164(normalizePhone(parsed.data.phone));
    } catch {
      return reply.status(400).send({ error: "Invalid phone" });
    }
    const code = randomOtp();
    const codeHash = await bcrypt.hash(code, 10);
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000);
    await prisma.otpChallenge.create({
      data: { phoneE164, role, codeHash, expiresAt },
    });
    await sendSms(phoneE164, `Care Companion code: ${code}`);
    return { sent: true };
  });

  app.post("/v1/auth/otp/verify", async (req, reply) => {
    const parsed = verifyOtpSchema.safeParse(req.body);
    if (!parsed.success) return reply.status(400).send({ error: "Invalid body" });
    let phoneE164: string;
    try {
      phoneE164 = toE164(normalizePhone(parsed.data.phone));
    } catch {
      return reply.status(400).send({ error: "Invalid phone" });
    }
    const challenge = await prisma.otpChallenge.findFirst({
      where: { phoneE164, consumedAt: null },
      orderBy: { createdAt: "desc" },
    });
    if (!challenge || challenge.expiresAt < new Date()) {
      return reply.status(400).send({ error: "Code expired or missing" });
    }
    if (challenge.attempts >= 8) {
      return reply.status(429).send({ error: "Too many attempts" });
    }
    const ok = await bcrypt.compare(parsed.data.code, challenge.codeHash);
    await prisma.otpChallenge.update({
      where: { id: challenge.id },
      data: { attempts: challenge.attempts + 1 },
    });
    if (!ok) return reply.status(400).send({ error: "Invalid code" });
    await prisma.otpChallenge.update({
      where: { id: challenge.id },
      data: { consumedAt: new Date() },
    });
    const user = await prisma.user.upsert({
      where: { phoneE164 },
      update: { role: challenge.role },
      create: { phoneE164, role: challenge.role },
    });
    const accessToken = signToken(user.id, user.role, user.phoneE164);
    return {
      accessToken,
      user: { id: user.id, phoneE164: user.phoneE164, role: user.role },
    };
  });

  app.get("/v1/guardian/profiles", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    if (payload.role !== Role.GUARDIAN) return reply.status(403).send({ error: "Forbidden" });
    const rows = await prisma.elderProfile.findMany({
      where: { guardianUserId: payload.sub },
      orderBy: { displayName: "asc" },
    });
    return {
      profiles: rows.map((p) => ({
        id: p.id,
        displayName: p.displayName,
        linkedElderPhoneE164: p.linkedElderPhoneE164,
        avatarIconKey: p.avatarIconKey,
        avatarBgArgb: p.avatarBgArgb,
        photoUrl: p.photoUrl,
      })),
    };
  });

  const createProfileSchema = z.object({
    displayName: z.string().min(1),
    linkedElderPhone: z.string().optional(),
    avatarIconKey: z.string().optional(),
    avatarBgArgb: z.number().int().optional(),
    photoUrl: z.string().url().optional().nullable(),
  });

  app.post("/v1/guardian/profiles", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    if (payload.role !== Role.GUARDIAN) return reply.status(403).send({ error: "Forbidden" });
    const parsed = createProfileSchema.safeParse(req.body);
    if (!parsed.success) return reply.status(400).send({ error: "Invalid body" });
    let linked: string | null = null;
    if (parsed.data.linkedElderPhone) {
      try {
        linked = toE164(normalizePhone(parsed.data.linkedElderPhone));
      } catch {
        return reply.status(400).send({ error: "Invalid linked elder phone" });
      }
    }
    const p = await prisma.elderProfile.create({
      data: {
        guardianUserId: payload.sub,
        displayName: parsed.data.displayName.trim(),
        linkedElderPhoneE164: linked,
        avatarIconKey: parsed.data.avatarIconKey ?? null,
        avatarBgArgb: parsed.data.avatarBgArgb ?? null,
        photoUrl: parsed.data.photoUrl ?? null,
      },
    });
    return { profile: { id: p.id, displayName: p.displayName, linkedElderPhoneE164: p.linkedElderPhoneE164 } };
  });

  app.delete("/v1/guardian/profiles/:profileId", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    if (payload.role !== Role.GUARDIAN) return reply.status(403).send({ error: "Forbidden" });
    const profileId = (req.params as { profileId: string }).profileId;
    const p = await prisma.elderProfile.findFirst({
      where: { id: profileId, guardianUserId: payload.sub },
    });
    if (!p) return reply.status(404).send({ error: "Not found" });
    await prisma.elderProfile.delete({ where: { id: profileId } });
    return { ok: true };
  });

  const contactSchema = z.object({
    name: z.string().min(1),
    phone: z.string().min(5),
    photoUrl: z.string().url().optional().nullable(),
  });

  app.get("/v1/guardian/profiles/:profileId/contacts", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    if (payload.role !== Role.GUARDIAN) return reply.status(403).send({ error: "Forbidden" });
    const profileId = (req.params as { profileId: string }).profileId;
    const p = await prisma.elderProfile.findFirst({
      where: { id: profileId, guardianUserId: payload.sub },
    });
    if (!p) return reply.status(404).send({ error: "Not found" });
    const rows = await prisma.contact.findMany({ where: { elderProfileId: profileId } });
    return {
      contacts: rows.map((c) => ({
        id: c.id,
        name: c.name,
        phoneE164: c.phoneE164,
        photoUrl: c.photoUrl,
      })),
    };
  });

  app.post("/v1/guardian/profiles/:profileId/contacts", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    if (payload.role !== Role.GUARDIAN) return reply.status(403).send({ error: "Forbidden" });
    const profileId = (req.params as { profileId: string }).profileId;
    const p = await prisma.elderProfile.findFirst({
      where: { id: profileId, guardianUserId: payload.sub },
    });
    if (!p) return reply.status(404).send({ error: "Not found" });
    const parsed = contactSchema.safeParse(req.body);
    if (!parsed.success) return reply.status(400).send({ error: "Invalid body" });
    let phoneE164: string;
    try {
      phoneE164 = toE164(normalizePhone(parsed.data.phone));
    } catch {
      return reply.status(400).send({ error: "Invalid phone" });
    }
    const c = await prisma.contact.create({
      data: {
        elderProfileId: profileId,
        name: parsed.data.name.trim(),
        phoneE164,
        photoUrl: parsed.data.photoUrl ?? null,
      },
    });
    return {
      contact: { id: c.id, name: c.name, phoneE164: c.phoneE164, photoUrl: c.photoUrl },
    };
  });

  app.delete("/v1/guardian/profiles/:profileId/contacts/:contactId", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    const { profileId, contactId } = req.params as { profileId: string; contactId: string };
    const p = await prisma.elderProfile.findFirst({
      where: { id: profileId, guardianUserId: payload.sub },
    });
    if (!p) return reply.status(404).send({ error: "Not found" });
    const c = await prisma.contact.findFirst({ where: { id: contactId, elderProfileId: profileId } });
    if (!c) return reply.status(404).send({ error: "Not found" });
    await prisma.contact.delete({ where: { id: contactId } });
    return { ok: true };
  });

  const scheduleSchema = z.object({
    mealLabel: z.string(),
    timeLabel: z.string(),
    mealTiming: z.enum(["BEFORE", "AFTER"]),
    withWater: z.boolean(),
    enabled: z.boolean(),
  });

  const medicineSchema = z.object({
    name: z.string().min(1),
    dosage: z.string().optional(),
    form: z.string().optional(),
    isActive: z.boolean().optional(),
    pillImageUrl: z.string().url().optional().nullable(),
    packetFrontUrl: z.string().url().optional().nullable(),
    packetBackUrl: z.string().url().optional().nullable(),
    schedules: z.array(scheduleSchema).optional(),
  });

  app.get("/v1/guardian/profiles/:profileId/medicines", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    if (payload.role !== Role.GUARDIAN) return reply.status(403).send({ error: "Forbidden" });
    const profileId = (req.params as { profileId: string }).profileId;
    const p = await prisma.elderProfile.findFirst({
      where: { id: profileId, guardianUserId: payload.sub },
    });
    if (!p) return reply.status(404).send({ error: "Not found" });
    const rows = await prisma.medicine.findMany({
      where: { elderProfileId: profileId },
      include: { schedules: true },
    });
    return {
      medicines: rows.map((m) => ({
        id: m.id,
        name: m.name,
        dosage: m.dosage,
        form: m.form,
        isActive: m.isActive,
        pillImageUrl: m.pillImageUrl,
        packetFrontUrl: m.packetFrontUrl,
        packetBackUrl: m.packetBackUrl,
        schedules: m.schedules.map((s) => ({
          id: s.id,
          mealLabel: s.mealLabel,
          timeLabel: s.timeLabel,
          mealTiming: s.mealTiming,
          withWater: s.withWater,
          enabled: s.enabled,
        })),
      })),
    };
  });

  app.post("/v1/guardian/profiles/:profileId/medicines", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    const profileId = (req.params as { profileId: string }).profileId;
    const p = await prisma.elderProfile.findFirst({
      where: { id: profileId, guardianUserId: payload.sub },
    });
    if (!p) return reply.status(404).send({ error: "Not found" });
    const parsed = medicineSchema.safeParse(req.body);
    if (!parsed.success) return reply.status(400).send({ error: "Invalid body" });
    const m = await prisma.medicine.create({
      data: {
        elderProfileId: profileId,
        name: parsed.data.name.trim(),
        dosage: parsed.data.dosage ?? "",
        form: parsed.data.form ?? "Tablet",
        isActive: parsed.data.isActive ?? true,
        pillImageUrl: parsed.data.pillImageUrl ?? null,
        packetFrontUrl: parsed.data.packetFrontUrl ?? null,
        packetBackUrl: parsed.data.packetBackUrl ?? null,
        schedules: {
          create:
            parsed.data.schedules?.map((s) => ({
              mealLabel: s.mealLabel,
              timeLabel: s.timeLabel,
              mealTiming: s.mealTiming === "AFTER" ? MealTiming.AFTER : MealTiming.BEFORE,
              withWater: s.withWater,
              enabled: s.enabled,
            })) ?? [],
        },
      },
      include: { schedules: true },
    });
    return { medicine: m };
  });

  app.put("/v1/guardian/medicines/:medicineId", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    const medicineId = (req.params as { medicineId: string }).medicineId;
    const existing = await prisma.medicine.findFirst({
      where: { id: medicineId, elderProfile: { guardianUserId: payload.sub } },
      include: { elderProfile: true },
    });
    if (!existing) return reply.status(404).send({ error: "Not found" });
    const parsed = medicineSchema.partial().safeParse(req.body);
    if (!parsed.success) return reply.status(400).send({ error: "Invalid body" });
    const d = parsed.data;
    const medicineUpdate: {
      name?: string;
      dosage?: string;
      form?: string;
      isActive?: boolean;
      pillImageUrl?: string | null;
      packetFrontUrl?: string | null;
      packetBackUrl?: string | null;
    } = {};
    if (d.name !== undefined) medicineUpdate.name = d.name.trim();
    if (d.dosage !== undefined) medicineUpdate.dosage = d.dosage;
    if (d.form !== undefined) medicineUpdate.form = d.form;
    if (d.isActive !== undefined) medicineUpdate.isActive = d.isActive;
    if (d.pillImageUrl !== undefined) medicineUpdate.pillImageUrl = d.pillImageUrl;
    if (d.packetFrontUrl !== undefined) medicineUpdate.packetFrontUrl = d.packetFrontUrl;
    if (d.packetBackUrl !== undefined) medicineUpdate.packetBackUrl = d.packetBackUrl;
    if (d.name !== undefined) medicineUpdate.name = d.name.trim();
    if (d.dosage !== undefined) medicineUpdate.dosage = d.dosage;
    if (d.form !== undefined) medicineUpdate.form = d.form;
    if (d.isActive !== undefined) medicineUpdate.isActive = d.isActive;
    if (d.pillImageUrl !== undefined) medicineUpdate.pillImageUrl = d.pillImageUrl;
    if (d.packetFrontUrl !== undefined) medicineUpdate.packetFrontUrl = d.packetFrontUrl;
    if (d.packetBackUrl !== undefined) medicineUpdate.packetBackUrl = d.packetBackUrl;
    const schedules = d.schedules;
    await prisma.$transaction(async (tx) => {
      if (Object.keys(medicineUpdate).length > 0) {
        await tx.medicine.update({ where: { id: medicineId }, data: medicineUpdate });
      }
      if (schedules) {
        await tx.medicineSchedule.deleteMany({ where: { medicineId } });
        await tx.medicineSchedule.createMany({
          data: schedules.map((s) => ({
            medicineId,
            mealLabel: s.mealLabel,
            timeLabel: s.timeLabel,
            mealTiming: s.mealTiming === "AFTER" ? MealTiming.AFTER : MealTiming.BEFORE,
            withWater: s.withWater,
            enabled: s.enabled,
          })),
        });
      }
    });
    const m = await prisma.medicine.findUnique({ where: { id: medicineId }, include: { schedules: true } });
    return { medicine: m };
  });

  app.delete("/v1/guardian/medicines/:medicineId", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    const medicineId = (req.params as { medicineId: string }).medicineId;
    const existing = await prisma.medicine.findFirst({
      where: { id: medicineId, elderProfile: { guardianUserId: payload.sub } },
    });
    if (!existing) return reply.status(404).send({ error: "Not found" });
    await prisma.medicine.delete({ where: { id: medicineId } });
    return { ok: true };
  });

  async function elderProfileForUser(phoneE164: string) {
    return prisma.elderProfile.findFirst({
      where: { linkedElderPhoneE164: phoneE164 },
    });
  }

  app.get("/v1/elder/me", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    if (payload.role !== Role.ELDER) return reply.status(403).send({ error: "Forbidden" });
    const profile = await elderProfileForUser(payload.phoneE164);
    if (!profile) return reply.status(404).send({ error: "No linked care profile yet" });
    return {
      profile: {
        id: profile.id,
        displayName: profile.displayName,
      },
    };
  });

  app.get("/v1/elder/contacts", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    if (payload.role !== Role.ELDER) return reply.status(403).send({ error: "Forbidden" });
    const profile = await elderProfileForUser(payload.phoneE164);
    if (!profile) return reply.status(404).send({ error: "No linked care profile" });
    const rows = await prisma.contact.findMany({ where: { elderProfileId: profile.id } });
    return {
      contacts: rows.map((c) => ({
        id: c.id,
        name: c.name,
        phoneE164: c.phoneE164,
        photoUrl: c.photoUrl,
      })),
    };
  });

  app.get("/v1/elder/medicines", async (req, reply) => {
    let payload: JwtPayload;
    try {
      payload = verifyBearer(req.headers.authorization);
    } catch {
      return reply.status(401).send({ error: "Unauthorized" });
    }
    if (payload.role !== Role.ELDER) return reply.status(403).send({ error: "Forbidden" });
    const profile = await elderProfileForUser(payload.phoneE164);
    if (!profile) return reply.status(404).send({ error: "No linked care profile" });
    const rows = await prisma.medicine.findMany({
      where: { elderProfileId: profile.id, isActive: true },
      include: { schedules: { where: { enabled: true } } },
    });
    return {
      medicines: rows.map((m) => ({
        id: m.id,
        name: m.name,
        dosage: m.dosage,
        form: m.form,
        schedules: m.schedules.map((s) => ({
          mealLabel: s.mealLabel,
          timeLabel: s.timeLabel,
          mealTiming: s.mealTiming,
          withWater: s.withWater,
        })),
      })),
    };
  });

  return app;
}

async function main() {
  const app = await buildApp();
  await app.listen({ host: "0.0.0.0", port: PORT });
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
