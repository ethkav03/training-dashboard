import { Router } from "express";
import passport from "passport";
import { OAuth2Client } from "google-auth-library";
import { env, isGoogleOAuthConfigured } from "../config/env.js";
import { signUserToken } from "../lib/jwt.js";
import { prisma } from "../lib/prisma.js";
import { upsertUserFromGoogleIdentity } from "../services/userService.js";
import { validateBody } from "../middleware/validate.js";
import { googleMobileSchema } from "../validation/auth.validation.js";
import { ApiError } from "../middleware/errorHandler.js";
import type { User } from "@prisma/client";

export const authRouter = Router();

// Used to verify Google ID tokens the Android app obtains via Credential
// Manager. The `audience` for verifyIdToken must be the same web
// GOOGLE_CLIENT_ID already configured for the browser OAuth flow -- the
// Android OAuth client (registered separately, keyed by the app's SHA-1
// fingerprint) requests an ID token whose audience is still that web client.
const googleIdTokenClient = isGoogleOAuthConfigured ? new OAuth2Client(env.googleClientId) : null;

// Dev-only bypass so the app is fully explorable before Google OAuth credentials are
// configured. Disabled whenever NODE_ENV=production.
if (env.nodeEnv !== "production") {
  authRouter.post("/dev-login", async (_req, res, next) => {
    try {
      const user = await prisma.user.upsert({
        where: { googleId: "dev-local-user" },
        update: {},
        create: {
          googleId: "dev-local-user",
          email: "dev@momentum.local",
          name: "Dev User",
        },
      });
      const token = signUserToken({ sub: user.id, email: user.email });
      res.json({ token });
    } catch (err) {
      next(err);
    }
  });
}

authRouter.get("/google", (req, res, next) => {
  if (!isGoogleOAuthConfigured) {
    return res
      .status(503)
      .json({ error: "GoogleOAuthNotConfigured", message: "Set GOOGLE_CLIENT_ID/SECRET in backend/.env" });
  }
  passport.authenticate("google", { scope: ["profile", "email"], session: false })(req, res, next);
});

authRouter.get(
  "/google/callback",
  (req, res, next) => {
    passport.authenticate("google", { session: false, failureRedirect: `${env.frontendUrl}/login?error=oauth` })(
      req,
      res,
      next
    );
  },
  (req, res) => {
    const user = req.user as User;
    const token = signUserToken({ sub: user.id, email: user.email });
    res.redirect(`${env.frontendUrl}/auth/callback?token=${token}`);
  }
);

// Mobile Google Sign-In exchange: the Android app does native Google Sign-In
// (Credential Manager) to get a Google ID token, then POSTs it here instead
// of doing a browser redirect dance. Verifies the token server-side and
// returns the same JWT shape the web callback issues, so both clients share
// one auth mechanism downstream.
authRouter.post("/google/mobile", validateBody(googleMobileSchema), async (req, res, next) => {
  try {
    if (!isGoogleOAuthConfigured || !googleIdTokenClient) {
      return next(new ApiError(503, "GoogleOAuthNotConfigured"));
    }
    const ticket = await googleIdTokenClient.verifyIdToken({
      idToken: req.body.idToken,
      audience: env.googleClientId,
    });
    const payload = ticket.getPayload();
    if (!payload?.sub || !payload.email) {
      return next(new ApiError(401, "InvalidGoogleToken"));
    }
    const user = await upsertUserFromGoogleIdentity({
      googleId: payload.sub,
      email: payload.email,
      name: payload.name ?? payload.email,
      avatarUrl: payload.picture ?? null,
    });
    res.json({ token: signUserToken({ sub: user.id, email: user.email }) });
  } catch (err) {
    next(err instanceof Error && !(err instanceof ApiError) ? new ApiError(401, "InvalidGoogleToken") : err);
  }
});

authRouter.post("/logout", (_req, res) => {
  res.status(204).end();
});

authRouter.get("/status", (_req, res) => {
  res.json({ googleOAuthConfigured: isGoogleOAuthConfigured });
});
