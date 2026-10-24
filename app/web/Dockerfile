# syntax=docker/dockerfile:1
# Nginx 静态前端：非特权端口 8080，反向代理 /api -> api:8000
# 基础镜像由 Dependabot 周期性升级（1.27 已 EOL，OS 包漏洞会卡 Trivy 门禁）
FROM nginx:1.31-alpine
# 构建时升级 OS 包：基础镜像 digest 可能滞后于安全修复，导致 Trivy 门禁报已修复的 CVE
# （hadolint 默认不建议 apk upgrade，此处为安全考虑显式豁免）
RUN apk upgrade --no-cache && rm -f /etc/nginx/conf.d/default.conf # hadolint ignore=DL3017
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY site /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/healthz >/dev/null 2>&1 || exit 1
