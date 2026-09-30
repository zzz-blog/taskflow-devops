package com.taskflow.api.web;

import com.taskflow.api.dto.TaskCreateRequest;
import com.taskflow.api.dto.TaskResponse;
import com.taskflow.api.dto.TaskStatsResponse;
import com.taskflow.api.dto.TaskUpdateRequest;
import com.taskflow.api.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务 CRUD 接口。
 * 注意：前端 Nginx 已把 /api 前缀剥离后转发，因此这里直接映射 /tasks。
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskResponse> list(
            @RequestParam(required = false) Boolean done,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return service.search(done, q, limit, offset);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskCreateRequest request) {
        return service.create(request);
    }

    @GetMapping("/stats")
    public TaskStatsResponse stats() {
        return service.stats();
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public TaskResponse update(@PathVariable long id, @Valid @RequestBody TaskUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
