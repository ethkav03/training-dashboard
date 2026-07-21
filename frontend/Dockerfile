# Dev image: same workspace-install pattern as backend/Dockerfile. Source is
# bind-mounted for hot reload; this image just bakes node_modules.
FROM node:24-alpine AS dev

WORKDIR /app

COPY package.json package-lock.json ./
COPY packages/shared/package.json packages/shared/package.json
COPY backend/package.json backend/package.json
COPY frontend/package.json frontend/package.json

RUN npm install

COPY packages/shared packages/shared
COPY frontend frontend

WORKDIR /app/frontend

EXPOSE 5173

CMD ["npm", "run", "dev", "--", "--host"]
