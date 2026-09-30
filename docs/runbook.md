# 值班 Runbook（告警处置手册）

> 目标：任何告警，值班同学照着做就能在 5 分钟内定位或止血。每条告警对应 `monitoring/prometheus/alerts.yml` 中的一条规则。

## 通用排查三板斧

1. **看大盘**：Grafana「TaskFlow 应用监控」——先确认是全局还是单实例问题
2. **看变更**：`kubectl rollout history deployment/api -n taskflow`，最近 30 分钟内是否有发布（变更引起的问题先回滚再说）
3. **看日志**：Grafana → Loki → `{container="taskflow-api-1"}`，按时间范围过滤 ERROR

---

## critical

### InstanceDown（实例抓取失败 2 分钟）

- **现象**：Prometheus `up == 0`，目标实例指标中断
- **排查**：
  - `kubectl get pods -n taskflow`：Pod 是否 CrashLoopBackOff / ImagePullBackOff
  - `kubectl describe pod <pod> -n taskflow`：Events 区块（探针失败？OOMKilled？镜像拉取失败？）
- **处置**：
  - OOMKilled → `kubectl top pod` 确认内存曲线，调大 limits 后重启
  - 镜像拉取失败 → 检查 GHCR 凭据与镜像 tag 是否存在
  - 无法快速定位 → `kubectl rollout undo deployment/api -n taskflow` 回滚

### NodeLowDiskSpace（磁盘剩余 < 10%）

- **现象**：`node_filesystem_avail_bytes` 持续走低
- **排查**：`df -h` + `du -sh /var/lib/docker /var/log` 定位大头
- **处置**：清理 Docker 无用镜像 `docker system prune -af`；检查日志卷是否被刷爆；确认 Loki/监控数据保留期配置

## warning

### APIHighErrorRate（5xx 错误率 > 5% 持续 5 分钟）

- **排查**：Grafana QPS 面板确认哪些状态码 → Loki 过滤 5xx 请求日志 → 常见原因：数据库连接池打满、DB 挂了（同时会有 readyz 503）、代码 bug（看最近发布）
- **处置**：DB 问题优先扩容连接/重启 PG；代码问题回滚

### APIHighP95Latency（P95 > 500ms 持续 10 分钟）

- **排查**：Grafana 分位延迟面板确认陡增时间点 → 是否伴随 CPU 飙升 → Loki 看慢请求集中在哪个接口
- **处置**：确认 HPA 是否已扩容（`kubectl get hpa api -n taskflow`）；临时手段：手动扩容 `kubectl scale deployment/api --replicas=4 -n taskflow`；根治：压测复现后优化慢 SQL/加索引

### NodeHighCPU（> 85% 持续 10 分钟）

- **排查**：`kubectl top nodes` + `kubectl top pods --sort-by=cpu` 找出吃 CPU 的 Pod；区分业务高峰还是异常
- **处置**：业务高峰 → 验证 HPA 行为、评估扩容节点；异常 → 结合日志定位（常见：死循环、压测未停）

### NodeLowMemory（可用内存 < 10%）

- **排查**：`kubectl top pods --sort-by=memory`；`dmesg | grep -i oom` 查历史 OOM
- **处置**：给大内存 Pod 设置合理 limits；确认 PostgreSQL 缓存配置；必要时加节点

---

## 值班交接要求

- 处置动作与时间点记录在值班群/工单，包括：告警触发/恢复时间、根因、动作、遗留风险
- 每条 P1/P2 告警恢复后 24h 内补充复盘（是否符合"告警 → 排查 → 止血 → 根治"四步）
