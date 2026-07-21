import { randomUUID } from "node:crypto";
import { Router } from "express";
import { isWhoopConfigured, env } from "../config/env.js";
import { requireAuth } from "../middleware/requireAuth.js";
import { signOAuthState, verifyOAuthState } from "../lib/jwt.js";
import { ApiError } from "../middleware/errorHandler.js";
import { buildAuthorizeUrl, connectWithCode, disconnectWhoop, syncWhoopData } from "../services/whoopService.js";

export const whoopRouter = Router();

// Called via an authenticated axios request (not a plain <a href>, unlike
// the Google flow) because the connect flow needs to know which Momentum
// user is asking -- a full-page navigation can't carry the Bearer header.
whoopRouter.get("/connect", requireAuth, (req, res, next) => {
  try {
    if (!isWhoopConfigured) {
      return res
        .status(503)
        .json({ error: "WhoopNotConfigured", message: "Set WHOOP_CLIENT_ID/SECRET in backend/.env" });
    }
    const state = signOAuthState({ sub: req.userId!, nonce: randomUUID() });
    res.json({ authorizeUrl: buildAuthorizeUrl(state) });
  } catch (err) {
    next(err);
  }
});

// Not behind requireAuth -- WHOOP's redirect carries no Bearer header at
// all. Auth comes entirely from verifying the signed `state` param.
whoopRouter.get("/callback", async (req, res) => {
  const { code, state, error } = req.query as { code?: string; state?: string; error?: string };

  if (error || !code || !state) {
    return res.redirect(`${env.frontendUrl}/settings?whoop=error&message=${encodeURIComponent(error ?? "missing_code")}`);
  }

  try {
    const { sub: userId } = verifyOAuthState(state);
    await connectWithCode(userId, code);
    res.redirect(`${env.frontendUrl}/settings?whoop=connected`);
  } catch (err) {
    const message = err instanceof Error ? err.message : "oauth_failed";
    res.redirect(`${env.frontendUrl}/settings?whoop=error&message=${encodeURIComponent(message)}`);
  }
});

whoopRouter.post("/sync", requireAuth, async (req, res, next) => {
  try {
    res.json(await syncWhoopData(req.userId!));
  } catch (err) {
    next(err);
  }
});

whoopRouter.delete("/", requireAuth, async (req, res, next) => {
  try {
    await disconnectWhoop(req.userId!);
    res.status(204).end();
  } catch (err) {
    next(err instanceof Error ? new ApiError(500, err.message) : err);
  }
});
