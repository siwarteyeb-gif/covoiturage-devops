# syntax=docker/dockerfile:1
# ---- Etape 1 : compilation Angular ----
FROM node:22.12-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps
COPY . .
RUN npx ng build --configuration production

# ---- Etape 2 : nginx sans privilege (utilisateur non root, port 8080) ----
FROM nginxinc/nginx-unprivileged:1.27.2-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/covoiturage-front/browser /usr/share/nginx/html
# Utilisateur non root (l'image "unprivileged" fournit l'utilisateur nginx, uid 101)
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -qO /dev/null http://127.0.0.1:8080/health || exit 1
