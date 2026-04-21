# syntax=docker/dockerfile:1.6
# ============================================================
# Stage 1: build with Maven (JDK 17, 缓存 ~/.m2 提速)
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先只复制 pom 以利用 Docker 层缓存
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

# 再复制源码并打包
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests package && \
    cp target/*.jar /build/app.jar

# ============================================================
# Stage 2: runtime (精简 JRE)
# ============================================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 非 root 用户
RUN groupadd --system app && useradd --system --gid app app && \
    mkdir -p /app/uploads && chown -R app:app /app

COPY --from=build --chown=app:app /build/app.jar /app/app.jar

ENV SERVER_PORT=8081 \
    UPLOADS_DIR=/app/uploads \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport"

EXPOSE 8081
VOLUME ["/app/uploads"]
USER app

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
