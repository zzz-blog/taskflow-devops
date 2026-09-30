package com.taskflow.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 创建任务请求。 */
public record TaskCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @Min(1) @Max(3) Integer priority) {

    public int resolvedPriority() {
        return priority == null ? 2 : priority;
    }
}
