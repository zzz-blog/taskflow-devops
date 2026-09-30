.PHONY: help test run-api run-web build up down logs restart smoke load-test k8s-apply k8s-dev k8s-prod k8s-diff helm-lint clean

# 常用命令封装：make help 查看全部
# Windows 建议在 Git Bash 中运行；或在 app/api 下直接用 mvnw.cmd
help: ## 显示帮助
	@grep -E '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "} {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

test: ## 编译 + 单元测试 + JaCoCo 覆盖率门禁
	cd app/api && ./mvnw -B verify

run-api: ## 本地启动 API（开发模式）
	cd app/api && ./mvnw spring-boot:run

run-web: ## 本地启动前端静态页（需要 API 已启动）
	cd app/web/site && python -m http.server 5500

build: ## 构建两个镜像
	docker build -t taskflow-api:local app/api
	docker build -t taskflow-web:local app/web

up: ## 启动全栈（应用 + 监控 + 日志）
	docker compose up -d --build

down: ## 停止全栈
	docker compose down

restart: ## 重启全栈
	docker compose down && docker compose up -d --build

logs: ## 跟踪容器日志
	docker compose logs -f --tail=100

smoke: ## 冒烟测试（需全栈已启动）
	bash scripts/smoke-test.sh

load-test: ## 简易压测（需全栈已启动）
	python scripts/load_test.py --url http://127.0.0.1:8080/api/tasks --duration 30 --concurrency 10

k8s-diff: ## 预览 K8s 生产环境变更
	kubectl diff -k k8s/overlays/prod || true

k8s-dev: ## 部署到 K8s dev 环境
	kubectl apply -k k8s/overlays/dev

k8s-prod: ## 部署到 K8s prod 环境
	kubectl apply -k k8s/overlays/prod

helm-lint: ## 校验 Helm Chart
	helm lint helm/taskflow

clean: ## Maven 清理
	cd app/api && ./mvnw -B -q clean
