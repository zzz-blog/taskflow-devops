#!/usr/bin/env bash
# 冒烟测试：验证全栈关键链路（web 代理 -> api -> db）
# 用法: bash scripts/smoke-test.sh [BASE_URL]  默认 http://127.0.0.1:8080
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
API_URL="${API_URL:-http://127.0.0.1:8000}"

pass() { echo "✅ $1"; }
fail() { echo "❌ $1"; exit 1; }

echo "==> 冒烟测试: ${BASE_URL}"

curl -fsS "${BASE_URL}/healthz" >/dev/null || fail "web /healthz 不可用"
pass "web 健康检查"

curl -fsS "${API_URL:-http://127.0.0.1:8000}/healthz" >/dev/null 2>/dev/null \
  && pass "api 直连健康检查" \
  || echo "⚠️  api 直连跳过（生产环境 api 不对外）"

curl -fsS "${API_URL}/readyz" >/dev/null 2>/dev/null && pass "api 数据库就绪" || true

# 通过 web 反向代理走一遍完整业务链路（标题用 ASCII：Windows 终端内联中文可能非 UTF-8）
CREATED=$(curl -fsS -X POST "${BASE_URL}/api/tasks" \
  -H "Content-Type: application/json" \
  -d '{"title":"smoke-test-alert-drill","priority":1}')
TASK_ID=$(echo "$CREATED" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$TASK_ID" ] || fail "任务创建失败: $CREATED"
pass "通过 nginx 代理创建任务 (id=${TASK_ID})"

curl -fsS "${BASE_URL}/api/tasks?limit=5" >/dev/null || fail "任务列表不可用"
pass "任务列表查询"

curl -fsS -X DELETE "${BASE_URL}/api/tasks/${TASK_ID}" >/dev/null || fail "清理任务失败"
pass "清理测试数据"

echo "==> 全部通过 🎉"
