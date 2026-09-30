# syntax=docker/dockerfile:1
# ---- 构建阶段：Maven 编译打包（.m2 走 BuildKit 缓存，重复构建显著提速）----
FROM maven:3-eclipse-temurin-26 AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && cp target/taskflow-api-*.jar target/app.jar

# ---- 运行阶段：Alpine JRE + 非 root 用户（busybox wget 提供健康检查，无需 apt）----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -u 10001 -G app appuser
WORKDIR /app
COPY --from=build /build/target/app.jar app.jar
USER 10001
EXPOSE 8000
# 容器内 JVM 内存感知：按容器限额取 75% 作为堆上限
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8000/healthz >/dev/null 2>&1 || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
