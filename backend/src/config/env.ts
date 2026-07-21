import "dotenv/config";

function required(name: string, fallback?: string): string {
  const value = process.env[name] ?? fallback;
  if (value === undefined) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

export const env = {
  nodeEnv: process.env.NODE_ENV ?? "development",
  port: Number(process.env.PORT ?? 4000),
  databaseUrl: required("DATABASE_URL"),
  jwtSecret: required("JWT_SECRET", "dev-insecure-secret-change-me"),
  jwtExpiresIn: process.env.JWT_EXPIRES_IN ?? "7d",
  googleClientId: process.env.GOOGLE_CLIENT_ID ?? "",
  googleClientSecret: process.env.GOOGLE_CLIENT_SECRET ?? "",
  googleCallbackUrl:
    process.env.GOOGLE_CALLBACK_URL ?? "http://localhost:4000/api/auth/google/callback",
  frontendUrl: process.env.FRONTEND_URL ?? "http://localhost:5173",
  // Insecure fallback so local dev works with zero setup, same pattern as jwtSecret.
  // Never rely on the fallback beyond localhost -- it's checked into no repo,
  // but it's also not a secret since it's public in this source file.
  encryptionKey: required("ENCRYPTION_KEY", "0".repeat(64)),
  whoopClientId: process.env.WHOOP_CLIENT_ID ?? "",
  whoopClientSecret: process.env.WHOOP_CLIENT_SECRET ?? "",
  whoopRedirectUri:
    process.env.WHOOP_REDIRECT_URI ?? "http://localhost:4000/api/integrations/whoop/callback",
};

export const isGoogleOAuthConfigured = Boolean(env.googleClientId && env.googleClientSecret);
export const isWhoopConfigured = Boolean(env.whoopClientId && env.whoopClientSecret);
