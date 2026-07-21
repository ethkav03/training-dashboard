import type { NextFunction, Request, Response } from "express";
import { verifyUserToken } from "../lib/jwt.js";
import { prisma } from "../lib/prisma.js";
import { ApiError } from "./errorHandler.js";

declare global {
  namespace Express {
    interface Request {
      userId?: string;
    }
  }
}

export async function requireAuth(req: Request, _res: Response, next: NextFunction) {
  const header = req.headers.authorization;
  if (!header?.startsWith("Bearer ")) {
    return next(new ApiError(401, "Unauthorized"));
  }
  try {
    const payload = verifyUserToken(header.slice("Bearer ".length));
    const user = await prisma.user.findUnique({ where: { id: payload.sub } });
    if (!user) {
      return next(new ApiError(401, "Unauthorized"));
    }
    req.userId = user.id;
    next();
  } catch {
    next(new ApiError(401, "Unauthorized"));
  }
}
