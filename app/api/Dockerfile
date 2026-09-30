# syntax=docker/dockerfile:1
# ---- 构建阶段：Maven 编译打包（.m2 走 BuildKit 缓存，重复构建显著提速）----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && cp target/taskflow-api-*.jar target/app.jar

# ---- 运行阶段：仅 JRE + 非 root 用户 ----
FROM eclipse-temurin:21-jre-noble
RUN apt-get update \
    # hadolint ignore=DL3008
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 appuser
WORKDIR /app
COPY --from=build /build/target/app.jar app.jar
USER 10001
EXPOSE 8000
# 容器内 JVM 内存感知：按容器限额取 75% 作为堆上限
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC"
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8000/healthz || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
