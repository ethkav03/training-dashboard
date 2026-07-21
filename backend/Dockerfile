# Dev image: installs workspace deps at the repo root so the @momentum/shared
# workspace link resolves, then runs the backend with hot reload. Source is
# bind-mounted by docker-compose.yml, so this image only needs to bake
# node_modules (including the Prisma client generated for linux-musl) and the
# Prisma schema needed for `prisma generate`/`migrate` at container start.
FROM node:24-alpine AS dev

RUN apk add --no-cache openssl

WORKDIR /app

COPY package.json package-lock.json ./
COPY packages/shared/package.json packages/shared/package.json
COPY backend/package.json backend/package.json
COPY frontend/package.json frontend/package.json

RUN npm install

COPY packages/shared packages/shared
COPY backend backend

WORKDIR /app/backend
RUN npx prisma generate

EXPOSE 4000

CMD ["sh", "-c", "npx prisma generate && npx prisma migrate deploy && npm run dev"]
