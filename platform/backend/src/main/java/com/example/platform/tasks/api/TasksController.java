package com.example.platform.tasks.api;

import com.example.platform.common.web.RequestContexts;
import com.example.platform.tasks.application.TasksFacade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            @Valid @RequestBody CreateTaskRequest request
    ) {
        return facade.createTask(
                workspaceId,
                RequestContexts.current().userId(),
                request.title(),
                request.description(),
                request.assigneeUserId()
        );
    }

    @GetMapping("/workspaces/{workspaceId}/tasks")
    public List<TasksFacade.TaskView> listTasks(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return facade.listTasks(workspaceId, RequestContexts.current().userId(), page, size);
    }

    @GetMapping("/tasks/{taskId}")
    public TasksFacade.TaskView getTask(@PathVariable String taskId) {
        return facade.getTask(taskId, RequestContexts.current().userId());
    }

    @PatchMapping("/tasks/{taskId}")
    public TasksFacade.TaskView updateTask(
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return facade.updateTask(
                taskId,
                RequestContexts.current().userId(),
                request.title(),
                request.description(),
                request.status(),
                request.assigneeUserId()
        );
    }

    @PostMapping("/tasks/{taskId}/comments")
    public TasksFacade.TaskCommentView addComment(
            @PathVariable String taskId,
            @Valid @RequestBody AddTaskCommentRequest request
    ) {
        return facade.addComment(taskId, RequestContexts.current().userId(), request.body());
    }

    public record CreateTaskRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 4000) String description,
            String assigneeUserId
    ) {
    }

    public record UpdateTaskRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 4000) String description,
            @NotBlank @Size(max = 32) String status,
            String assigneeUserId
    ) {
    }

    public record AddTaskCommentRequest(@NotBlank @Size(max = 4000) String body) {
    }
}
