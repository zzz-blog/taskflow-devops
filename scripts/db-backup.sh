#!/usr/bin/env bash
# PostgreSQL 备份脚本：docker exec + pg_dump，保留最近 7 份
# 可配合 crontab 使用（K8s 环境建议改为 CronJob + 对象存储）：
#   0 2 * * * /opt/taskflow/scripts/db-backup.sh
set -euo pipefail

cd "$(dirname "$0")/.."

BACKUP_DIR="backups"
KEEP=7
STAMP=$(date +%Y%m%d-%H%M%S)

mkdir -p "$BACKUP_DIR"
echo "==> 备份 PostgreSQL -> ${BACKUP_DIR}/taskflow-${STAMP}.sql.gz"
docker compose exec -T postgres \
  pg_dump -U "${POSTGRES_USER:-taskflow}" "${POSTGRES_DB:-taskflow}" \
  | gzip > "${BACKUP_DIR}/taskflow-${STAMP}.sql.gz"

echo "==> 清理 ${KEEP} 天前的旧备份"
ls -1t "${BACKUP_DIR}"/taskflow-*.sql.gz 2>/dev/null | tail -n +$((KEEP + 1)) | xargs -r rm --

echo "==> 完成：$(du -h "${BACKUP_DIR}/taskflow-${STAMP}.sql.gz" | cut -f1)"
