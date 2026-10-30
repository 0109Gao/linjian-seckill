# ============================================================
# 林间秒杀 · 后端镜像（多阶段构建）
#
# 阶段一：用 Maven 编译打包（复用本地已下载的依赖层，加快 CI）
# 阶段二：用精简 JRE 运行，减小镜像体积、降低攻击面
# ============================================================

# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 先只拷 pom，让 Maven 先解析依赖并缓存到镜像层。
# 源码改动不会让这层失效，CI 里能省下大部分下载时间。
COPY pom.xml .
COPY seckill-common/pom.xml seckill-common/pom.xml
COPY seckill-security/pom.xml seckill-security/pom.xml
COPY seckill-domain/pom.xml seckill-domain/pom.xml
COPY seckill-web/pom.xml seckill-web/pom.xml
RUN mvn -q -DskipTests dependency:go-offline

# 再拷源码编译
COPY seckill-common/src seckill-common/src
COPY seckill-security/src seckill-security/src
COPY seckill-domain/src seckill-domain/src
COPY seckill-web/src seckill-web/src
RUN mvn -q -DskipTests package

# ---- 运行阶段 ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# 用非 root 用户运行（安全基线）
RUN groupadd -r seckill && useradd -r -g seckill seckill

COPY --from=build /build/seckill-web/target/seckill-web.jar /app/seckill-web.jar
RUN chown -R seckill:seckill /app

USER seckill

# JVM 参数：
# -XX:MaxRAMPercentage 适配容器内存上限，避免默认按物理机内存分配堆
# -XX:+ExitOnOutOfMemoryError 让 OOM 时容器直接退出，交给编排层重启，而不是僵住
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/seckill-web.jar"]
