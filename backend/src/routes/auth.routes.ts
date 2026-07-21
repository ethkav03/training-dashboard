import { Router } from "express";
import passport from "passport";
import { env, isGoogleOAuthConfigured } from "../config/env.js";
import { signUserToken } from "../lib/jwt.js";
import { prisma } from "../lib/prisma.js";
import type { User } from "@prisma/client";

export const authRouter = Router();

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

authRouter.post("/logout", (_req, res) => {
  res.status(204).end();
});

authRouter.get("/status", (_req, res) => {
  res.json({ googleOAuthConfigured: isGoogleOAuthConfigured });
});
