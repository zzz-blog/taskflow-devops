package com.taskflow.api.repository;

import com.taskflow.api.domain.Task;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByOrderByPriorityAscIdDesc(Pageable pageable);

    List<Task> findByDoneOrderByPriorityAscIdDesc(boolean done, Pageable pageable);

    List<Task> findByTitleContainingIgnoreCaseOrderByPriorityAscIdDesc(String q, Pageable pageable);

    List<Task> findByDoneAndTitleContainingIgnoreCaseOrderByPriorityAscIdDesc(
            boolean done, String q, Pageable pageable);

    long countByDone(boolean done);

    @Query("select t.priority as priority, count(t) as cnt from Task t group by t.priority")
    List<PriorityCount> countByPriority();

    /** JPQL 聚合查询的接口投影。 */
    interface PriorityCount {
        Integer getPriority();

        Long getCnt();
    }
}
