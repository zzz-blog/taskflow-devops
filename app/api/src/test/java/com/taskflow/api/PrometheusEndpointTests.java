package com.taskflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Prometheus 指标端点测试。
 * Actuator 端点不走 MockMvc 的处理器映射，因此用随机端口 + 真实 HTTP 验证。
 * 注意：Boot 3.4 起测试上下文默认关闭指标导出，需要 @AutoConfigureObservability 显式开启。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class PrometheusEndpointTests {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void prometheusMetricsExposed() {
        // 先产生一次业务请求，确保 http_server_requests 指标已生成
        rest.getForEntity("/tasks", String.class);

        ResponseEntity<String> resp = rest.getForEntity("/actuator/prometheus", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("http_server_requests_seconds_count");
    }
}
