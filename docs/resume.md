# 简历写法与面试问答（必读）

> 这个仓库不只是代码，它是你的面试弹药库。本文告诉你：简历上怎么写、面试官会追问什么、你怎么答。

## 一、上简历前，先做这 5 件事（让项目"活"起来）

1. **fork/push 到自己的 GitHub**，让 README 的 CI 徽章真的变绿——面试官点进去看到真实的 Actions 运行记录，含金量完全不同。
2. **把占位符换成你的**：镜像仓库名、Jenkins registry 地址、告警 webhook（企业微信/钉钉/飞书机器人 5 分钟就能接）。
3. **跑一遍全栈并截图**：Grafana 看板、ArgoCD/GitHub Actions 运行页、`kubectl get pods`，放进 `docs/images/` 并在 README 引用。
4. **跑一次压测记录数据**：`python scripts/load_test.py --url http://127.0.0.1:8080/api/tasks --duration 60 --concurrency 20`，把 RPS/P95 写进简历数字。
5. **改动一两个业务点**（比如加一个接口 + 测试），确保每一个技术细节你都亲手碰过——面试最怕"项目很漂亮但一问三不知"。

## 二、简历项目描述（直接可用模板）

> **TaskFlow —— 云原生 DevOps 端到端实践平台**（个人项目，含完整文档与监控截图）

- 基于 **Java 21 + Spring Boot 3** 实现任务管理微服务：JPA 数据访问（PostgreSQL 生产 / H2 本地双配置）、Actuator 双健康探针、Micrometer + Prometheus 指标暴露、springdoc OpenAPI 3 文档、结构化 JSON 日志；配套 JUnit 5 + MockMvc 集成测试（JaCoCo 行覆盖率 92%）。
- 设计并实现端到端 CI/CD 流水线（GitHub Actions）：PR 合入前自动执行配置规范检查（hadolint/kubeconform/yamllint）、单元测试（JUnit 5 + JaCoCo 覆盖率门禁 80%）、双镜像构建与 Trivy 安全扫描（CRITICAL/HIGH 阻断合入），主分支合入后自动推送镜像至 GHCR 并以 **GitOps 模式回写生产清单**，全流程发布耗时约 3 分钟、全程零人工干预。
- 基于 **Kustomize 多环境差异管理 + Helm Chart** 完成 Kubernetes 编排：滚动更新零停机（maxUnavailable=0）、存活/就绪双探针、HPA 基于 CPU 自动扩缩容（2→6 副本）、PostgreSQL 以 StatefulSet+PVC 持久化，全部清单通过 kubeconform Schema 校验。
- 搭建完整可观测体系：Prometheus + Grafana + Alertmanager + Loki/Promtail，实现 QPS/P95 延迟/错误率三大黄金指标看板（Grafana 看板即代码）、**8 条分级告警规则**（critical/warning 分级路由 + 告警抑制）并配套值班 Runbook；压测验证接口 P95 < xx ms（填入你的实测数据）。
- 落地安全与成本实践：镜像多阶段构建 + 非 root 运行 + K8s securityContext 最小权限、Dependabot 依赖自动升级、Terraform 管理阿里云资源（VPC/安全组/ECS 开机自带 k3s）、Ansible Playbook 一键从裸机到可部署集群。

**写法要点**：每条都是「动词 + 技术关键词 + 量化结果」；数字（80%、2→6、3 分钟）来自本项目真实配置，你可以按实测调整。

## 三、面试高频追问与答题要点

### CI/CD

**Q: 你们的发布流程是什么？怎么回滚？**
> PR 阶段跑全量门禁（lint/test/scan），merge 后 CD 推镜像（sha+latest 双标签）并 `kustomize edit set image` 回写 prod overlay 提交到 Git，集群侧同步清单完成滚动更新。回滚 = `git revert` 那条 bump 提交，ArgoCD 自动回滚；兜底 `kubectl rollout undo`。强调"清单即事实源、发布可审计"。

**Q: 为什么用 Trivy 门禁而不是扫完不拦？**
> 漏洞修复"事后安排"等于不修。门禁策略：CRITICAL/HIGH 且**有修复版本**才失败（`--ignore-unfixed`），避免基础镜像历史漏洞把流水线彻底卡死——安全与交付效率的平衡。

**Q: [skip ci] 的作用？**
> CD 自己 bump 清单的提交如果不跳过会再次触发流水线，形成提交循环。GitHub Actions 原生识别 commit message 中的 `[skip ci]`。

### Kubernetes

**Q: liveness 和 readiness 的区别？配错会怎样？**
> liveness 管"进程是否存活"（失败重启容器），readiness 管"能否接流量"（失败摘除 Service 后端）。本项目 `/readyz` 会检查数据库连通性而 `/healthz` 不检查——如果 DB 抖动时用 liveness 检查 DB，所有 Pod 会被反复重启造成雪崩；正确姿势是只摘流量不重启。

**Q: HPA 怎么工作的？压测时怎么验证？**
> 依赖 metrics-server 采集 Pod CPU，超过 70% 目标触发扩容（min2/max6）。用仓库自带 `scripts/load_test.py` 压测，观察 `kubectl get hpa -w` 的 REPLICAS 变化与 Grafana CPU 曲线。还要能讲 `stabilizationWindowSeconds` 缩容防抖。

**Q: RollingUpdate 怎么保证零停机？**
> `maxUnavailable: 0` + readinessProbe：新副本就绪前旧副本不下线，Service 只把流量发给 ready 的 Pod。要能指出：没有 readinessProbe 的零停机配置是假的。

**Q: StatefulSet 和 Deployment 的区别？**
> 稳定网络标识（headless service DNS）+ 稳定存储（volumeClaimTemplates 每个 Pod 独立 PVC）+ 有序部署，适合数据库这类有状态负载。

### Java / Spring Boot 专项

**Q: Spring Boot 应用在 K8s 里怎么做健康检查？为什么 liveness 不检查数据库？**
> 项目同时暴露 `/healthz`（liveness，只确认进程活着）与 `/readyz`（readiness，通过 JdbcTemplate 检查数据库连通性），底层复用 Actuator 的 liveness/readiness health groups。若 liveness 也检查数据库，DB 抖动会让所有 Pod 被反复重启造成雪崩；正确姿势是只让 readiness 失败、摘除流量。

**Q: Java 容器化有什么要注意的？**
> JVM 必须感知容器限额：用 `-XX:MaxRAMPercentage=75` 而非写死 `-Xmx`，否则按物理机内存算堆会 OOMKilled；项目还开启了 ZGC、优雅停机（`server.shutdown: graceful` 配合 terminationGracePeriodSeconds），镜像侧用多阶段构建 + BuildKit 缓存 `.m2` 加速重复构建。

**Q: Micrometer 和 Prometheus 的关系？**
> Micrometer 是应用指标门面（SLF4J 之于日志），`micrometer-registry-prometheus` 把指标转成 Prometheus 文本格式，经 Actuator 的 `/actuator/prometheus` 暴露；自动带出 HTTP 请求直方图（`http_server_requests_seconds_*`）、JVM、连接池等指标。

### 可观测性

**Q: 告警规则怎么设计的？怎么避免告警疲劳？**
> 分 availability/resources 两组、critical/warning 两个级别；持续时长 `for` 消抖（如 5m）；Alertmanager 按 severity 分组路由、critical 缩短 repeat_interval、`inhibit_rules` 在实例掉线时抑制其衍生告警；每条告警对应 Runbook 的一条处置 SOP。衡量标准：每条告警都应可执行，不可执行的告警删掉。

**Q: Prometheus 的四大指标类型？你的看板用了哪些？**
> Counter（http_requests_total 算 QPS/错误率）、Histogram（请求耗时 bucket 算 P50/P95/P99）、Gauge（内存/磁盘）、Summary。要会写 `histogram_quantile(0.95, sum(rate(..._bucket[5m])) by (le))` 这类 PromQL。

**Q: 为什么选 Loki 而不是 ELK？**
> Loki 只索引标签不索引正文，存储成本低一个量级；与 Promtail/Grafana 同生态；日志量不大时 ELK 的全文检索优势用不上。适合"低成本、标签化检索"场景。

### IaC / 自动化

**Q: Terraform 和 Ansible 的分工？**
> Terraform 声明式管云资源生命周期（VPC/安全组/ECS，state 记录现实），Ansible 过程式管机器内配置与部署。本项目 Terraform 创建的 ECS 通过 user_data 开机自动装 k3s，Ansible 再负责集群内部署，通过 outputs/inventory 衔接。

**Q: Terraform state 为什么重要？**
> state 是"期望 vs 现实"的对照表，多人协作必须远程化（OSS/S3 + 锁），否则会互相覆盖。本仓库注释里给了 OSS backend 配置。

### 应急场景题（结合本项目 Runbook）

**Q: 线上 5xx 突增，你的第一反应？**
> 先看 Grafana 确认范围（全局/单实例）→ 看最近发布（30 分钟内发布优先回滚）→ Loki 按 status 过滤日志定位根因 → 处置后补复盘。强调"先止血后根因"。

## 四、按目标岗位的侧重建议

| 目标岗位 | 重点展示 |
|----------|----------|
| DevOps/SRE | CI/CD 门禁设计、K8s 排障、告警与 Runbook、HPA 压测数据 |
| 运维开发 | 在此基础上加自动化：把 Ansible 扩成多角色、给流水线加质量报表 |
| 平台工程 | 扩展 ArgoCD ApplicationSet、金丝雀发布（Argo Rollouts）、Secret 管理升级 |

## 五、进阶加分项（有余力再做）

1. 部署 ArgoCD 并截图 Applications 页面，把「GitOps 回写」闭环变成真实链路
2. Chaos Mesh 注入 Pod 故障，截图告警触发 → 验证监控有效性
3. 把监控栈用 `kube-prometheus-stack` Helm 装进集群，展示 ServiceMonitor 生效
4. 用 GitHub Environments 给 prod 加手动审批（环境保护规则），讲"发布审批流"
