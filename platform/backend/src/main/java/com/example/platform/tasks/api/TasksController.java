package com.example.platform.tasks.api;

import com.example.platform.common.web.RequestContexts;
import com.example.platform.tasks.application.TasksFacade;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class TasksController {

    private final TasksFacade facade;

    public TasksController(TasksFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/workspaces/{workspaceId}/tasks")
    public TasksFacade.TaskView createTask(
            @PathVariable String workspaceId,
            @RequestBody CreateTaskRequest request
    ) {
        return facade.createTask(workspaceId, request.title());
    }

    @GetMapping("/workspaces/{workspaceId}/tasks")
    public List<TasksFacade.TaskView> listTasks(@PathVariable String workspaceId) {
        return facade.listTasks(workspaceId);
    }

    @GetMapping("/tasks/{taskId}")
    public TasksFacade.TaskView getTask(@PathVariable String taskId) {
        return facade.getTask(taskId);
    }

    @PatchMapping("/tasks/{taskId}")
    public TasksFacade.TaskView updateTask(@PathVariable String taskId) {
        return facade.updateTask(taskId);
    }

    @PostMapping("/tasks/{taskId}/comments")
    public TasksFacade.TaskCommentView addComment(
            @PathVariable String taskId,
            @RequestBody AddTaskCommentRequest request
    ) {
        return facade.addComment(taskId, RequestContexts.current().userId(), request.body());
    }

    public record CreateTaskRequest(@NotBlank String title) {
    }

    public record AddTaskCommentRequest(@NotBlank String body) {
    }
}
