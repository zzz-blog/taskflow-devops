package com.taskflow.api.dto;

import java.util.Map;

/** 任务统计响应体。 */
public record TaskStatsResponse(long total, long done, long todo, Map<Integer, Long> byPriority) {
}
