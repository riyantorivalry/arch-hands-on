package com.example.platform.tasks.infrastructure;

import com.example.platform.tasks.domain.TaskCommentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentRepository extends JpaRepository<TaskCommentEntity, String> {

    List<TaskCommentEntity> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
