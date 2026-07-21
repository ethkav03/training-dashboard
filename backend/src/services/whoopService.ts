import type { WhoopSyncResultDto } from "@momentum/shared";
import { env } from "../config/env.js";
import { prisma } from "../lib/prisma.js";
import { decrypt, encrypt } from "../lib/crypto.js";

const AUTHORIZE_URL = "https://api.prod.whoop.com/oauth/oauth2/auth";
const TOKEN_URL = "https://api.prod.whoop.com/oauth/oauth2/token";

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

// WHOOP's token endpoint takes a JSON body, not the more typical
// x-www-form-urlencoded most OAuth2 servers use -- verified against docs.
async function requestToken(body: Record<string, string>): Promise<WhoopTokenResponse> {
  const res = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
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

/**
 * Real fetch + field mapping (recovery/sleep -> RecoveryRecord, workouts ->
 * TrainingSession) lands in the WHOOP data-sync sprint -- this proves the
 * connect -> sync -> disconnect round trip end to end, including token
 * refresh, before that mapping is built.
 */
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

    const result: WhoopSyncResultDto = {
      status: "SUCCESS",
      syncedAt: new Date().toISOString(),
      recoveryRecordsSynced: 0,
      recoveryRecordsSkippedManualEdit: 0,
      trainingSessionsSynced: 0,
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
