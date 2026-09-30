# syntax=docker/dockerfile:1
# Frontend image: builds the React app, serves it with nginx.
# nginx also forwards the API paths (/api/, /appointment/, /uploads/) to the backend container,
# so the browser only talks to one address and no CORS setup is needed.
# Build context: hospital-frontend/

# ---------- build ----------
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
# Baked into the bundle at build time. "/" = call the API on the same address the page came from
# (nginx forwards it). Set a full URL instead to call a backend somewhere else.
ARG REACT_APP_API_URL=/
ENV REACT_APP_API_URL=$REACT_APP_API_URL
RUN npm run build

# ---------- serve ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
