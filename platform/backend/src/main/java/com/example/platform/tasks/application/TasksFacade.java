package com.example.platform.tasks.application;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TasksFacade {

    public TaskView createTask(String workspaceId, String title) {
        return new TaskView("task-" + title.toLowerCase().replace(" ", "-"), workspaceId, title, "TODO", null);
    }

    public List<TaskView> listTasks(String workspaceId) {
        return List.of(new TaskView("task-platform-bootstrap", workspaceId, "Platform Bootstrap", "IN_PROGRESS", "user-dev"));
    }

    public TaskView getTask(String taskId) {
        return new TaskView(taskId, "workspace-dev", "Platform Bootstrap", "IN_PROGRESS", "user-dev");
    }

    public TaskView updateTask(String taskId) {
        return new TaskView(taskId, "workspace-dev", "Platform Bootstrap", "DONE", "user-dev");
    }

    public TaskCommentView addComment(String taskId, String userId, String body) {
        return new TaskCommentView("task-comment-1", taskId, userId, body);
    }

    public record TaskView(String taskId, String workspaceId, String title, String status, String assigneeUserId) {
    }

    public record TaskCommentView(String commentId, String taskId, String authorUserId, String body) {
    }
}
