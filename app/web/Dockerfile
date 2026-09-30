# syntax=docker/dockerfile:1
# Nginx 静态前端：非特权端口 8080，反向代理 /api -> api:8000
FROM nginx:1.27-alpine
RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY site /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/healthz >/dev/null 2>&1 || exit 1
