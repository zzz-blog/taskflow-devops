package com.taskflow.api.service;

import com.taskflow.api.domain.Task;
import com.taskflow.api.dto.TaskCreateRequest;
import com.taskflow.api.dto.TaskResponse;
import com.taskflow.api.dto.TaskStatsResponse;
import com.taskflow.api.dto.TaskUpdateRequest;
import com.taskflow.api.exception.TaskNotFoundException;
import com.taskflow.api.repository.TaskRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private static final int DEFAULT_LIMIT = 50;

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> search(Boolean done, String q, int limit, int offset) {
        int size = Math.clamp(limit, 1, 200);
        // offset 对齐到页边界；演示数据量下语义等价
        Pageable page = PageRequest.of(Math.max(offset, 0) / size, size);
        List<Task> tasks;
        if (done == null && q == null) {
            tasks = repository.findAllByOrderByPriorityAscIdDesc(page);
        } else if (done == null) {
            tasks = repository.findByTitleContainingIgnoreCaseOrderByPriorityAscIdDesc(q, page);
        } else if (q == null) {
            tasks = repository.findByDoneOrderByPriorityAscIdDesc(done, page);
        } else {
            tasks = repository.findByDoneAndTitleContainingIgnoreCaseOrderByPriorityAscIdDesc(done, q, page);
        }
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public TaskResponse create(TaskCreateRequest request) {
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.resolvedPriority());
        return TaskResponse.from(repository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse get(long id) {
        return TaskResponse.from(find(id));
    }

    @Transactional
    public TaskResponse update(long id, TaskUpdateRequest request) {
        Task task = find(id);
        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.done() != null) {
            task.setDone(request.done());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        return TaskResponse.from(repository.save(task));
    }

    @Transactional
    public void delete(long id) {
        repository.delete(find(id));
    }

    @Transactional(readOnly = true)
    public TaskStatsResponse stats() {
        long total = repository.count();
        long done = repository.countByDone(true);
        Map<Integer, Long> byPriority = new LinkedHashMap<>();
        repository.countByPriority().forEach(row -> byPriority.put(row.getPriority(), row.getCnt()));
        return new TaskStatsResponse(total, done, total - done, byPriority);
    }

    private Task find(long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }
}
