# ============================================================
# 林间秒杀 · 前端 + Nginx 镜像（多阶段）
#
# 阶段一：分别构建 user / admin 两个前端
# 阶段二：把产物拷进 nginx，配好反向代理与静态托管
# ============================================================

# ---- 构建 user 前端 ----
FROM node:20-alpine AS build-user
WORKDIR /build
COPY frontend-user/package*.json ./
RUN npm ci || npm install
COPY frontend-user ./
RUN npm run build

# ---- 构建 admin 前端 ----
FROM node:20-alpine AS build-admin
WORKDIR /build
COPY frontend-admin/package*.json ./
RUN npm ci || npm install
COPY frontend-admin ./
RUN npm run build

# ---- 运行阶段：nginx ----
FROM nginx:1.27-alpine
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build-user /build/dist /usr/share/nginx/html/user
COPY --from=build-admin /build/dist /usr/share/nginx/html/admin
EXPOSE 80
