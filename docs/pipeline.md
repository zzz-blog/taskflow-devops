# CI/CD 流水线详解

## 两条流水线的职责边界

| | CI (`ci.yml`) | CD (`cd.yml`) |
|---|---|---|
| 触发 | PR / push main | push main / 打 tag v* |
| 职责 | 质量门禁：代码规范、测试、构建、漏洞扫描 | 发布：推镜像、GitOps 回写、（可选）部署 |
| 失败后果 | 无法合入 | 回滚到上一版本清单即可 |

## CI 阶段设计（PR 合入前全量门禁）

```
lint ──┐
       ├──► build-scan（构建 + Trivy + 推 GHCR）
test ──┘
```

1. **lint job（< 2min）**：并行做 5 类检查
   - `yamllint` 全仓 YAML
   - `hadolint` Dockerfile 规范（非 root、依赖固定版本等）
   - `kubeconform` K8s 清单 schema 校验（防止 YAML 拼错上不了线）
   - `helm lint` Chart 模板校验
   - `terraform fmt/validate` IaC 规范
2. **test job**：Maven 单测（JUnit 5）+ JaCoCo 行覆盖率 ≥ 80% 硬门禁（`./mvnw verify` 内置 check 规则）
3. **build-scan job**：双镜像构建 → Trivy 扫描（CRITICAL/HIGH 且有修复版本即失败）→ main 分支推 GHCR（`sha` + `latest` 双标签）

**设计要点**：PR 阶段只构建不推送（验证可构建性）；`concurrency` 取消同分支过期任务，省算力。

## CD 流程（GitOps 模式）

```
push main
  └─► build-push：构建镜像 → 扫描 → 推送 ghcr.io/<repo>/taskflow-{api,web}:{sha,latest}
        └─► gitops-bump：kustomize edit set image → overlays/prod 的 newTag=sha
              └─► git commit "[skip ci] chore(release): bump prod images to xxx" → push
                    └─► （集群侧）ArgoCD 检测到清单变更 → 自动 sync 滚动更新
```

- **为什么回写 Git 而不是直接 kubectl**：清单即事实源，发布记录天然可审计、可回滚（git revert 一条命令）。
- **[skip ci] 防循环触发**：CD 自己的提交不再触发 CI。
- **tag 事件**：`v*` tag 额外推送语义化版本镜像，作为正式发布产物。
- **可选 deploy job**：配置 `KUBE_CONFIG` secret 后可跳过 GitOps 直接部署（兼容没有 ArgoCD 的环境）。
- **镜像双标签**：`sha` 保证可追溯（哪个 commit 构建的），`latest` 方便本地拉取。

## 质量门禁清单（可量化，面试可讲）

| 门禁 | 阈值 | 工具 |
|------|------|------|
| 单元测试覆盖率 | ≥ 80% | JaCoCo |
| 镜像漏洞 | CRITICAL/HIGH 且有修复版本 = 0 | Trivy |
| K8s 清单 | schema 全部合法 | kubeconform |
| Dockerfile | hadolint 规则全过 | hadolint |
| IaC | fmt + validate 通过 | terraform |

## Jenkins 等价实现

`jenkins/Jenkinsfile` 提供声明式流水线（Lint → Test → Build → Scan → Push → Deploy），适合对接公司已有 Jenkins；凭证通过 `withCredentials` 注入而非明文。

## 发布回滚 SOP

1. GitOps 模式：`git revert` 那条 bump 提交 → ArgoCD 自动回滚
2. 手动模式：`kustomize edit set image ... :<上一个 sha>` + `kubectl apply -k k8s/overlays/prod`
3. 兜底：`kubectl rollout undo deployment/api -n taskflow`
