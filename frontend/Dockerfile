# ---- Étape 1 : compilation Angular ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --legacy-peer-deps
COPY . .
RUN npx ng build

# ---- Étape 2 : serveur nginx ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/covoiturage-front/browser /usr/share/nginx/html
EXPOSE 80
