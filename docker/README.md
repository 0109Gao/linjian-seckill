# docker/ 目录说明

## 现状：双轨并行

宿主机已装的 MySQL / Redis / RabbitMQ **不卸载**，容器另起一套走备用端口。
两套互不干扰，可以随时对照，也可以随时 `down` 掉容器而不影响 W4 的联调。

| 组件 | 宿主机原生 | 容器 |
|---|---|---|
| MySQL | `localhost:3306` | `localhost:3307`（root / 123456789） |
| Redis | `localhost:6379` | `localhost:6380`（无密码） |
| RabbitMQ AMQP | `localhost:5672` | `localhost:5673`（seckill / seckill123） |
| RabbitMQ 控制台 | http://localhost:15672 | http://localhost:15673 |

## 常用命令

```powershell
# 启动（首次会拉镜像 + 执行 schema.sql 与 seed-dev.sql，约 2~5 分钟）
docker compose -f docker/docker-compose.middleware.yml up -d

# 看状态（等三个服务都 healthy 再连）
docker compose -f docker/docker-compose.middleware.yml ps

# 看某个服务的日志
docker compose -f docker/docker-compose.middleware.yml logs -f mysql

# 停止（保留数据卷）
docker compose -f docker/docker-compose.middleware.yml down

# 停止并清空数据（下次启动会重新执行初始化 SQL）
docker compose -f docker/docker-compose.middleware.yml down -v
```

## 让后端连到容器

容器的端口与宿主机原生不同，必须覆盖环境变量，否则后端会静默连回宿主机那套
（`REDIS_HOST` 与 `REDIS_PORT` 已在 `application.yml` 里改成占位符，见该文件注释）。

```powershell
$env:MYSQL_PORT         = "3307"
$env:REDIS_PORT         = "6380"
$env:RABBITMQ_PORT      = "5673"
$env:RABBITMQ_USERNAME  = "seckill"
$env:RABBITMQ_PASSWORD  = "seckill123"

cd backend
java -jar seckill-web/target/seckill-web.jar
```

注意 PowerShell 里环境变量在**新开窗口**才生效，且不继承给已启动的进程；
要还原就关掉窗口重开。

## 三个容易踩的坑

1. **RabbitMQ 不能用 guest**。guest 被 RabbitMQ 限制为「仅本机连接」，
   宿主机进程连容器映射出来的 5673 属于跨网络，会被拒并报 `ACCESS_REFUSED`，
   而错误信息不会提 loopback 限制。所以编排里建了专用账号 `seckill`。
2. **排序规则要一致**。`schema.sql` 用 `utf8mb4_general_ci`，容器若默认成
   `utf8mb4_0900_ai_ci`，跨表 JOIN 会抛 `Illegal mix of collations`。
   编排里已用 `--collation-server` 钉死。
3. **初始化 SQL 只跑一次**。`/docker-entrypoint-initdb.d/` 里的脚本仅在数据卷
   为空时执行。改了 `seed-dev.sql` 想重跑，必须 `down -v` 清卷。
4. **中文乱码（双重编码）**。MySQL 8.4 官方镜像 initdb 时客户端默认
   `character_set_client=latin1`（容器 locale 是 C/POSIX），会把 SQL 文件里的
   UTF-8 中文当 latin1 读入，写进 utf8mb4 列时双重编码成 mojibake。
   `--character-set-server` 只影响服务端不影响客户端，`--init-connect` 对 root
   无效。**修复**：`schema.sql` 与 `seed-dev.sql` 文件第一行加 `SET NAMES utf8mb4;`
   （在任何 DDL 之前）。已加，勿删。这是 docker-library/mysql#131 的老问题。

## 完整编排（W5，上线级交付形态）

`docker-compose.full.yml` 一条命令拉起全栈（默认端口，交付态）：

```powershell
docker compose -f docker/docker-compose.full.yml up -d --build
```

包含 5 个服务：

| 服务 | 镜像/端口 | 说明 |
|---|---|---|
| mysql | `mysql:8.4` / 3306 | 容器内默认端口，initdb 自动建库建表 |
| redis | `redis:7-alpine` / 6379 | db0 登录态 / db1 库存 |
| rabbitmq | `rabbitmq:4-management` / 5672+15672 | 专用账号 seckill/seckill123 |
| seckill-web | `linjian-seckill-web:1.0.0` / 8081 | 后端，连容器内中间件（服务名直连） |
| nginx | `linjian-seckill-frontend:1.0.0` / 80 | 托管 user(/user/) + admin(/admin/)，反代 /api |

访问入口：

- 用户端：http://localhost/user/
- 管理端：http://localhost/admin/
- 根路径 `/` 自动 302 到 `/user/`

两个前端在构建时已配 vite `base`（`/user/`、`/admin/`）与 router 的
`createWebHistory` base，因此 history 路由刷新不会 404（nginx `try_files` 兜底）。

### 镜像构建

- 后端：`backend/Dockerfile`（多阶段：`maven:3.9-eclipse-temurin-21` 编译 →
  `eclipse-temurin:21-jre` 运行，非 root 用户，`-XX:MaxRAMPercentage`）
- 前端：`docker/frontend.Dockerfile`（分别构建 user/admin 后拷进 nginx）

### 双轨说明

- `docker-compose.middleware.yml`：**开发态**，备用端口 3307/6380/5673，
  与宿主机原生中间件并行，不抢默认端口。
- `docker-compose.full.yml`：**交付态**，默认端口，中间件只在容器内，后端服务名直连。

两者都复用同名数据卷（`sk-mysql-data` 等），切换时注意：同一时刻只跑一套，
否则端口冲突。切 full 前先 `docker compose -f docker/docker-compose.middleware.yml down`。

## 三个容易踩的坑（开发态也适用）
