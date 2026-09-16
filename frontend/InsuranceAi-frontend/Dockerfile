# --- Build stage -------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /app

# Dependencies first so they're cached across builds whenever only source changes.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build


# --- Runtime stage -------------------------------------------------------
# Static files served by nginx, which also reverse-proxies /api to the backend container --
# the app's own axios client already calls relative /api paths (see src/api/client.js), so no
# frontend code or build-time env var is needed for this to work.
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=5 \
    CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1
