package com.taskflow.api.exception;

/** 任务不存在 -> 由 GlobalExceptionHandler 映射为 404。 */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(long id) {
        super("task not found: " + id);
    }
}
