package com.example.platform.tasks.infrastructure;

import com.example.platform.tasks.domain.TaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, String> {

    List<TaskEntity> findByWorkspaceIdOrderByUpdatedAtDesc(String workspaceId);
}
