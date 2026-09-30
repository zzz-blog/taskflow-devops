package com.taskflow.api.web;

import com.taskflow.api.exception.TaskNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理：统一返回 RFC 7807 ProblemDetail。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    public ProblemDetail notFound(TaskNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "task not found");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalid(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "invalid request"
                : ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
