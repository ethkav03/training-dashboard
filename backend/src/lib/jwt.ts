import jwt from "jsonwebtoken";
import { env } from "../config/env.js";

export interface JwtPayload {
  sub: string;
  email: string;
}

export function signUserToken(payload: JwtPayload): string {
  return jwt.sign(payload, env.jwtSecret, { expiresIn: env.jwtExpiresIn as jwt.SignOptions["expiresIn"] });
}

export function verifyUserToken(token: string): JwtPayload {
  return jwt.verify(token, env.jwtSecret) as JwtPayload;
}

export interface OAuthStatePayload {
  sub: string;
  nonce: string;
}

/**
 * A self-contained, signed `state` param for third-party OAuth connect flows
 * (WHOOP, etc.) -- deliberately not stored server-side. nodemon restarts the
 * backend on every source save in dev, so an in-memory state map would break
 * any consent flow left mid-transit while editing code; a signed token needs
 * no storage and survives restarts.
 */
export function signOAuthState(payload: OAuthStatePayload): string {
  return jwt.sign(payload, env.jwtSecret, { expiresIn: "10m" });
}

export function verifyOAuthState(token: string): OAuthStatePayload {
  return jwt.verify(token, env.jwtSecret) as OAuthStatePayload;
}
