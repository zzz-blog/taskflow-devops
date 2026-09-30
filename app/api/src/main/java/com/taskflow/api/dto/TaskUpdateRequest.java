package com.taskflow.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** 更新任务请求：所有字段可选，仅更新显式传入的字段。 */
public record TaskUpdateRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 2000) String description,
        Boolean done,
        @Min(1) @Max(3) Integer priority) {
}
