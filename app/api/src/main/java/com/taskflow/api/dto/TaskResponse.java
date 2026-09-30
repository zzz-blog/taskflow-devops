package com.taskflow.api.dto;

import com.taskflow.api.domain.Task;
import java.time.Instant;

/** 任务响应体。 */
public record TaskResponse(
        long id,
        String title,
        String description,
        boolean done,
        int priority,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.isDone(),
                task.getPriority(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
