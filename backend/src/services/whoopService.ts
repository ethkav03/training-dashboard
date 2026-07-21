import type { ActivityType, WhoopSyncResultDto } from "@momentum/shared";
import { env } from "../config/env.js";
import { prisma } from "../lib/prisma.js";
import { decrypt, encrypt } from "../lib/crypto.js";
import { scoreToReadinessLevel } from "./recoveryService.js";
import {
  upsertExternalRecoveryRecord,
  upsertExternalTrainingSession,
  type ExternalSyncResult,
} from "./externalSyncService.js";
import { computeTrainingLoad } from "./trainingService.js";

const AUTHORIZE_URL = "https://api.prod.whoop.com/oauth/oauth2/auth";
const TOKEN_URL = "https://api.prod.whoop.com/oauth/oauth2/token";
const API_BASE = "https://api.prod.whoop.com/developer";

// Verified against developer.whoop.com/docs: "offline" is required to receive
// a refresh_token at all; the rest are the data scopes we actually use.
const SCOPES = [
  "offline",
  "read:recovery",
  "read:cycles",
  "read:sleep",
  "read:workout",
  "read:profile",
  "read:body_measurement",
];

export function buildAuthorizeUrl(state: string): string {
  const params = new URLSearchParams({
    client_id: env.whoopClientId,
    redirect_uri: env.whoopRedirectUri,
    response_type: "code",
    scope: SCOPES.join(" "),
    state,
  });
  return `${AUTHORIZE_URL}?${params.toString()}`;
}

interface WhoopTokenResponse {
  access_token: string;
  refresh_token?: string;
  expires_in: number;
  token_type: string;
  scope: string;
}

// WHOOP's OAuth2 server is Ory Hydra (same host/path pattern:
// /oauth/oauth2/auth, /oauth/oauth2/token) -- its token endpoint follows
// standard OAuth2 (RFC 6749 4.1.3): application/x-www-form-urlencoded, not
// JSON. A prior pass here sent a JSON body instead; Hydra's form parser
// found zero fields in it and rejected every real request with "The POST
// body can not be empty," confirmed against a live WHOOP account.
async function requestToken(body: Record<string, string>): Promise<WhoopTokenResponse> {
  const res = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams(body).toString(),
  });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`WHOOP token request failed (${res.status}): ${text}`);
  }
  return res.json() as Promise<WhoopTokenResponse>;
}

function exchangeCodeForTokens(code: string): Promise<WhoopTokenResponse> {
  return requestToken({
    grant_type: "authorization_code",
    code,
    client_id: env.whoopClientId,
    client_secret: env.whoopClientSecret,
    redirect_uri: env.whoopRedirectUri,
  });
}

function refreshTokens(refreshToken: string): Promise<WhoopTokenResponse> {
  return requestToken({
    grant_type: "refresh_token",
    refresh_token: refreshToken,
    client_id: env.whoopClientId,
    client_secret: env.whoopClientSecret,
    scope: "offline",
  });
}

async function persistTokens(
  userId: string,
  tokens: WhoopTokenResponse,
  opts: { isInitialConnect: boolean }
): Promise<void> {
  const access = encrypt(tokens.access_token);
  const refresh = tokens.refresh_token ? encrypt(tokens.refresh_token) : undefined;
  const now = new Date();
  const tokenExpiresAt = new Date(now.getTime() + tokens.expires_in * 1000);

  await prisma.integrationConnection.upsert({
    where: { userId_provider: { userId, provider: "WHOOP" } },
    update: {
      accessTokenCiphertext: access.ciphertext,
      accessTokenIv: access.iv,
      accessTokenAuthTag: access.authTag,
      // WHOOP may not rotate the refresh token on every refresh -- only
      // overwrite it when a new one actually comes back.
      ...(refresh
        ? { refreshTokenCiphertext: refresh.ciphertext, refreshTokenIv: refresh.iv, refreshTokenAuthTag: refresh.authTag }
        : {}),
      scope: tokens.scope,
      tokenExpiresAt,
      ...(opts.isInitialConnect ? { connectedAt: now, disconnectedAt: null } : {}),
    },
    create: {
      userId,
      provider: "WHOOP",
      accessTokenCiphertext: access.ciphertext,
      accessTokenIv: access.iv,
      accessTokenAuthTag: access.authTag,
      refreshTokenCiphertext: refresh?.ciphertext,
      refreshTokenIv: refresh?.iv,
      refreshTokenAuthTag: refresh?.authTag,
      scope: tokens.scope,
      tokenExpiresAt,
      connectedAt: now,
    },
  });
}

export async function connectWithCode(userId: string, code: string): Promise<void> {
  const tokens = await exchangeCodeForTokens(code);
  await persistTokens(userId, tokens, { isInitialConnect: true });
}

export async function disconnectWhoop(userId: string): Promise<void> {
  const existing = await prisma.integrationConnection.findUnique({
    where: { userId_provider: { userId, provider: "WHOOP" } },
  });
  if (!existing) return;
  // Null the token columns and mark disconnected -- deliberately does NOT
  // touch WeightEntry/TrainingSession/RecoveryRecord rows already synced;
  // there is no FK from this row to any of them, so history is untouched
  // by construction, not by extra disconnect logic.
  await prisma.integrationConnection.update({
    where: { id: existing.id },
    data: {
      accessTokenCiphertext: null,
      accessTokenIv: null,
      accessTokenAuthTag: null,
      refreshTokenCiphertext: null,
      refreshTokenIv: null,
      refreshTokenAuthTag: null,
      tokenExpiresAt: null,
      disconnectedAt: new Date(),
    },
  });
}

/** Refreshes and persists a new access token if the current one is expired or about to be. */
async function getValidAccessToken(userId: string): Promise<string | null> {
  const conn = await prisma.integrationConnection.findUnique({
    where: { userId_provider: { userId, provider: "WHOOP" } },
  });
  if (!conn || conn.disconnectedAt || !conn.refreshTokenCiphertext || !conn.accessTokenCiphertext) {
    return null;
  }

  const needsRefresh = !conn.tokenExpiresAt || conn.tokenExpiresAt.getTime() < Date.now() + 60_000;
  if (!needsRefresh) {
    return decrypt({
      ciphertext: conn.accessTokenCiphertext,
      iv: conn.accessTokenIv!,
      authTag: conn.accessTokenAuthTag!,
    });
  }

  const refreshToken = decrypt({
    ciphertext: conn.refreshTokenCiphertext,
    iv: conn.refreshTokenIv!,
    authTag: conn.refreshTokenAuthTag!,
  });
  const tokens = await refreshTokens(refreshToken);
  await persistTokens(userId, tokens, { isInitialConnect: false });
  return tokens.access_token;
}

// ---- Data fetch + mapping (verified against developer.whoop.com/docs) ----

interface WhoopRecovery {
  cycle_id: number;
  sleep_id: number;
  created_at: string;
  score_state: string;
  score?: {
    recovery_score: number;
    resting_heart_rate: number;
    hrv_rmssd_milli: number;
  };
}

interface WhoopSleep {
  id: number;
  start: string;
  end: string;
  score_state: string;
  score?: {
    stage_summary: {
      total_light_sleep_time_milli: number;
      total_slow_wave_sleep_time_milli: number;
      total_rem_sleep_time_milli: number;
    };
    sleep_performance_percentage: number;
  };
}

interface WhoopCycle {
  id: number;
  start: string;
  end: string | null;
  score_state: string;
  score?: {
    strain: number;
  };
}

interface WhoopWorkout {
  id: number;
  start: string;
  end: string;
  sport_name: string;
  score_state: string;
  score?: {
    strain: number;
    kilojoule: number;
    average_heart_rate: number;
  };
}

interface PaginatedResponse<T> {
  records: T[];
  next_token: string | null;
}

async function fetchAllPages<T>(path: string, accessToken: string, start: Date): Promise<T[]> {
  const records: T[] = [];
  let nextToken: string | null = null;
  const MAX_PAGES = 20;

  for (let page = 0; page < MAX_PAGES; page++) {
    const params = new URLSearchParams({ start: start.toISOString(), limit: "25" });
    if (nextToken) params.set("nextToken", nextToken);

    const res = await fetch(`${API_BASE}${path}?${params.toString()}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!res.ok) {
      const text = await res.text().catch(() => "");
      throw new Error(`WHOOP request to ${path} failed (${res.status}): ${text}`);
    }
    const body = (await res.json()) as PaginatedResponse<T>;
    records.push(...body.records);
    nextToken = body.next_token;
    if (!nextToken) break;
  }

  return records;
}

// Keyed by WHOOP's `sport_name` (lowercased) -- sport IDs aren't documented
// with a stable public enum, so matching on the human-readable name is the
// more robust option. Team sports never map to MATCH: WHOOP has no
// opponent/result data, so MatchDetail couldn't be populated anyway.
const SPORT_NAME_TO_ACTIVITY_TYPE: Record<string, ActivityType> = {
  weightlifting: "GYM",
  "functional fitness": "GYM",
  powerlifting: "GYM",
  running: "RUNNING",
  cycling: "CYCLING",
  walking: "WALKING",
  hiking: "WALKING",
  soccer: "TEAM_SPORT_TRAINING",
  basketball: "TEAM_SPORT_TRAINING",
  "american football": "TEAM_SPORT_TRAINING",
  hockey: "TEAM_SPORT_TRAINING",
  rugby: "TEAM_SPORT_TRAINING",
  "australian football": "TEAM_SPORT_TRAINING",
  baseball: "TEAM_SPORT_TRAINING",
};

function mapSportToActivityType(sportName: string): ActivityType {
  return SPORT_NAME_TO_ACTIVITY_TYPE[sportName.trim().toLowerCase()] ?? "OTHER";
}

function dayStart(date: Date): Date {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d;
}

const KJ_TO_KCAL = 0.239006;

export async function syncWhoopData(userId: string): Promise<WhoopSyncResultDto> {
  const connection = await prisma.integrationConnection.findUnique({
    where: { userId_provider: { userId, provider: "WHOOP" } },
  });
  if (!connection || connection.disconnectedAt) {
    return {
      status: "ERROR",
      syncedAt: new Date().toISOString(),
      recoveryRecordsSynced: 0,
      recoveryRecordsSkippedManualEdit: 0,
      trainingSessionsSynced: 0,
      errorMessage: "WHOOP is not connected",
    };
  }

  await prisma.integrationConnection.update({ where: { id: connection.id }, data: { lastSyncStatus: "SYNCING" } });

  try {
    const accessToken = await getValidAccessToken(userId);
    if (!accessToken) throw new Error("Could not obtain a valid WHOOP access token");

    // First sync bounds lookback to 30 days; subsequent syncs pick up from
    // the last successful sync. A recovery cycle only exists once the
    // preceding night's sleep has closed, so a same-day "sync now" right
    // after connecting will correctly show nothing for today yet.
    const since = connection.lastSyncAt ?? new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);

    const [recoveries, sleeps, workouts, cycles] = await Promise.all([
      fetchAllPages<WhoopRecovery>("/v2/recovery", accessToken, since),
      fetchAllPages<WhoopSleep>("/v2/activity/sleep", accessToken, since),
      fetchAllPages<WhoopWorkout>("/v2/activity/workout", accessToken, since),
      fetchAllPages<WhoopCycle>("/v2/cycle", accessToken, since),
    ]);

    const sleepById = new Map(sleeps.map((s) => [s.id, s]));

    // WHOOP's app shows a fresh recovery alongside *yesterday's* (the just-
    // completed) cycle strain, not the still-accumulating current cycle --
    // so for a given recovery we look up its own cycle's position in
    // chronological order and take the strain of the cycle immediately
    // before it.
    const cyclesByStart = [...cycles].sort(
      (a, b) => new Date(a.start).getTime() - new Date(b.start).getTime()
    );
    const cycleIndexById = new Map(cyclesByStart.map((c, i) => [c.id, i]));

    function yesterdaysStrain(cycleId: number): number | null {
      const index = cycleIndexById.get(cycleId);
      if (index == null || index === 0) return null;
      const previousCycle = cyclesByStart[index - 1];
      if (previousCycle.score_state !== "SCORED" || !previousCycle.score) return null;
      return Math.round(previousCycle.score.strain * 10) / 10;
    }

    let recoverySynced = 0;
    let recoverySkipped = 0;
    for (const recovery of recoveries) {
      if (recovery.score_state !== "SCORED" || !recovery.score) continue;

      const sleep = sleepById.get(recovery.sleep_id);
      const sleepScored = sleep?.score_state === "SCORED" && sleep.score;
      const sleepHours = sleepScored
        ? (sleep!.score!.stage_summary.total_light_sleep_time_milli +
            sleep!.score!.stage_summary.total_slow_wave_sleep_time_milli +
            sleep!.score!.stage_summary.total_rem_sleep_time_milli) /
          3_600_000
        : null;
      const sleepScore = sleepScored ? Math.round(sleep!.score!.sleep_performance_percentage) : null;

      const readinessScore = Math.round(recovery.score.recovery_score);
      const result: ExternalSyncResult = await upsertExternalRecoveryRecord({
        userId,
        source: "WHOOP",
        externalId: String(recovery.cycle_id),
        // Recovery is generated once the preceding sleep closes, so its
        // created_at reliably lands on the same calendar day as the sleep
        // it's paired with -- using it as the shared bucket keeps both
        // merged into one RecoveryRecord row instead of landing a day apart.
        date: dayStart(new Date(recovery.created_at)),
        sleepHours,
        sleepScore,
        strain: yesterdaysStrain(recovery.cycle_id),
        restingHr: Math.round(recovery.score.resting_heart_rate),
        hrv: recovery.score.hrv_rmssd_milli,
        readinessScore,
        readinessLevel: scoreToReadinessLevel(readinessScore),
      });
      if (result === "skipped_manual_edit") recoverySkipped++;
      else recoverySynced++;
    }

    let trainingSynced = 0;
    for (const workout of workouts) {
      if (workout.score_state !== "SCORED" || !workout.score) continue;

      const durationMin = Math.round((new Date(workout.end).getTime() - new Date(workout.start).getTime()) / 60_000);
      const intensity = Math.max(1, Math.min(10, Math.round((workout.score.strain / 21) * 10)));

      await upsertExternalTrainingSession({
        userId,
        source: "WHOOP",
        externalId: String(workout.id),
        type: mapSportToActivityType(workout.sport_name),
        date: new Date(workout.start),
        durationMin,
        intensity,
        caloriesBurned: Math.round(workout.score.kilojoule * KJ_TO_KCAL),
        avgHeartRate: Math.round(workout.score.average_heart_rate),
        trainingLoad: computeTrainingLoad(durationMin, intensity),
      });
      trainingSynced++;
    }

    const result: WhoopSyncResultDto = {
      status: "SUCCESS",
      syncedAt: new Date().toISOString(),
      recoveryRecordsSynced: recoverySynced,
      recoveryRecordsSkippedManualEdit: recoverySkipped,
      trainingSessionsSynced: trainingSynced,
    };
    await prisma.integrationConnection.update({
      where: { id: connection.id },
      data: { lastSyncAt: new Date(), lastSyncStatus: "SUCCESS", lastSyncError: null },
    });
    return result;
  } catch (err) {
    const message = err instanceof Error ? err.message : "Unknown sync error";
    await prisma.integrationConnection.update({
      where: { id: connection.id },
      data: { lastSyncStatus: "ERROR", lastSyncError: message },
    });
    return {
      status: "ERROR",
      syncedAt: new Date().toISOString(),
      recoveryRecordsSynced: 0,
      recoveryRecordsSkippedManualEdit: 0,
      trainingSessionsSynced: 0,
      errorMessage: message,
    };
  }
}
