package com.taskflow.api;

import com.taskflow.api.domain.Task;
import com.taskflow.api.repository.TaskRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TaskFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskFlowApplication.class, args);
    }

    /** 空库时写入示例数据，方便首次演示（幂等）。 */
    @Bean
    ApplicationRunner seedSampleData(TaskRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(task("接入 Prometheus 监控", 1, true));
                repository.save(task("为流水线增加安全扫描门禁", 1, false));
                repository.save(task("编写值班 Runbook", 3, false));
            }
        };
    }

    private static Task task(String title, int priority, boolean done) {
        Task task = new Task();
        task.setTitle(title);
        task.setPriority(priority);
        task.setDone(done);
        return task;
    }
}
