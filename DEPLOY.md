# 部署与运维说明

本项目支持 **Docker Compose 一键部署**，包含 4 个服务：

| 服务 | 镜像 | 作用 | 默认对外端口 |
|---|---|---|---|
| `mysql` | `mysql:8.0` | 持久化数据 | 3306 |
| `redis` | `redis:7-alpine` | 缓存 / Token 黑名单 / 登录限流 / 浏览量计数 | 6379 |
| `backend` | 由 `backend/backend/Dockerfile` 构建 | Spring Boot API | 仅容器内暴露 8081 |
| `frontend` | 由 `frontend/community-skill-help-frontend/Dockerfile` 构建 | Nginx 托管前端，反向代理 `/api` `/uploads` | 8080 |

---

## 1. 环境要求

- Docker 24+
- Docker Compose v2（`docker compose` 命令）
- 构建期需要外网（拉 Maven / npm 依赖）

## 2. 快速开始

```bash
# 1) 准备环境变量
cp .env.example .env
# 按需编辑：数据库密码、Redis 密码、端口等

# 2) 构建并启动（首次构建较慢，会下 JDK 和 npm 依赖）
docker compose up -d --build

# 3) 查看状态
docker compose ps
docker compose logs -f backend
```

启动完成后访问：

- 前端：<http://localhost:8080> （由 `FRONTEND_PORT` 控制）
- 后端健康检查：<http://localhost:8080/api/ping>（经前端 Nginx 代理）

前端的所有 `/api/*` 请求会由 Nginx 反向代理到 `backend:8081`，浏览器侧只需要访问前端端口。

## 3. 目录与数据卷

| Volume | 挂载点 | 用途 |
|---|---|---|
| `mysql_data` | `/var/lib/mysql` | MySQL 数据 |
| `redis_data` | `/data` | Redis AOF 持久化 |
| `backend_uploads` | `/app/uploads` | 用户上传的图片 |

销毁方式：

```bash
docker compose down          # 停止
docker compose down -v       # 停止并清空所有数据卷（危险！）
```

## 4. Redis 在业务里做了什么

项目对 Redis 有真实使用（不是空壳），全部通过 `CacheService` 统一封装：

1. **Token 黑名单**
   - `POST /api/auth/logout` 会把当前 JWT 存入 `auth:token:bl:{token}`，TTL 等于该 token 自身剩余有效期。
   - `JwtAuthInterceptor` 每次请求都检查黑名单，被拉黑的 token 直接 401。
2. **登录失败限流**
   - 按手机号计数 `auth:login:fail:{phone}`，默认 5 次失败锁 5 分钟（`auth:login:lock:{phone}`）。
   - 参数可通过 `LOGIN_MAX_FAIL` / `LOGIN_LOCK_SECONDS` 环境变量调整。
3. **技能详情缓存**
   - Key: `skill:detail:{id}`，TTL 默认 600 秒（`CACHE_SKILL_DETAIL_TTL`）。
   - 技能更新 / 上下架 / 删除时自动 `evict`。
4. **浏览量 Redis 计数 + 懒回写**
   - Key: `skill:view:{id}`，每次详情接口命中 `INCR`；每累计 10 次把增量回写到 MySQL，避免热点写放大。
   - 详情接口返回值 = DB 基线 + Redis 尾数，保持实时观感。

## 5. 常用运维命令

```bash
# 只重建后端
docker compose up -d --build backend

# 进入后端容器
docker exec -it csh-backend sh

# 进入 Redis CLI
docker exec -it csh-redis redis-cli
# 若设置了密码
docker exec -it csh-redis redis-cli -a "$REDIS_PASSWORD"

# 进入 MySQL
docker exec -it csh-mysql mysql -uroot -p

# 查看应用日志
docker compose logs -f backend
```

## 6. 生产优化建议

- **`JwtUtil` 里的 SECRET** 目前是硬编码的，生产环境请改成从环境变量读取。
- 使用前置 HTTPS（Caddy / Traefik / 云厂商 SLB）终止 TLS，compose 中的 Nginx 只做同源分流。
- 数据库建议独立托管，compose 中的 mysql 仅适用于开发 / 小规模部署。
- 为 Redis 设置密码（`.env` 的 `REDIS_PASSWORD`），禁止外网直连。
- 首次部署成功后，建议备份 `mysql_data` 与 `backend_uploads`。

## 7. 故障排查

| 现象 | 可能原因 | 处理 |
|---|---|---|
| 后端启动失败，日志报 `Communications link failure` | MySQL 尚未就绪 | 稍候；compose 已配 healthcheck，一般会自恢复 |
| 前端能打开但 `/api` 404 | Nginx 代理未生效 | 检查 `frontend/community-skill-help-frontend/nginx.conf`，确保 `proxy_pass http://backend:8081;` |
| 登录提示"登录失败次数过多" | 触发 Redis 限流 | 等 5 分钟 或 `redis-cli DEL auth:login:lock:{phone}` |
| 图片上传后 404 | uploads 卷未挂载或权限异常 | `docker exec -it csh-backend ls -la /app/uploads` 检查 |
| 修改技能后详情仍是旧数据 | 缓存未失效（异常路径） | 手动 `redis-cli DEL skill:detail:{id}` 或等 TTL 到期 |
