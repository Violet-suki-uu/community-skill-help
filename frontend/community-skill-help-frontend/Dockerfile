# syntax=docker/dockerfile:1.6
# ============================================================
# Stage 1: build Vue app with Vite
# ============================================================
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund

COPY . .

# 生产构建：走同源，走 Nginx 反向代理到 /api 与 /uploads，避免镜像硬编码后端地址
ENV VITE_API_BASE_URL=""
RUN npm run build

# ============================================================
# Stage 2: runtime with Nginx
# ============================================================
FROM nginx:1.27-alpine

RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/app.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
