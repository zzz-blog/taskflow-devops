package com.taskflow.api.web;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * K8s 探针端点：
 * - /healthz 存活探针（liveness）：只确认进程活着，不检查外部依赖，避免依赖抖动引发重启风暴
 * - /readyz  就绪探针（readiness）：检查数据库连通性，失败时摘除流量
 */
@RestController
public class ProbeController {

    private final JdbcTemplate jdbc;
    private final String version;
    private final String environment;

    public ProbeController(JdbcTemplate jdbc, org.springframework.core.env.Environment env) {
        this.jdbc = jdbc;
        this.version = env.getProperty("taskflow.app-version", "1.0.0");
        this.environment = env.getProperty("taskflow.environment", "local");
    }

    @GetMapping("/healthz")
    public Map<String, String> liveness() {
        return Map.of("status", "ok", "version", version, "env", environment);
    }

    @GetMapping("/readyz")
    public ResponseEntity<Map<String, String>> readiness() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "ready"));
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "not ready", "reason", "database unavailable"));
        }
    }
}
