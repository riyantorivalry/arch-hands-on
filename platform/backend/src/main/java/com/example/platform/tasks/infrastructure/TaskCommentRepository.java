package com.example.platform.tasks.infrastructure;

import com.example.platform.tasks.domain.TaskCommentEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface TaskCommentRepository extends ReactiveCrudRepository<TaskCommentEntity, String> {

    Flux<TaskCommentEntity> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
