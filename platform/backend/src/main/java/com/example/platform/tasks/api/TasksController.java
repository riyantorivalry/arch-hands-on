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
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api")
public class TasksController {

    private final TasksFacade facade;

    public TasksController(TasksFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/workspaces/{workspaceId}/tasks")
    public Mono<TasksFacade.TaskView> createTask(
            @PathVariable String workspaceId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.createTask(
                        workspaceId,
                        context.userId(),
                        request.title(),
                        request.description(),
                        request.assigneeUserId()
                ));
    }

    @GetMapping("/workspaces/{workspaceId}/tasks")
    public Mono<List<TasksFacade.TaskView>> listTasks(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.listTasks(workspaceId, context.userId(), page, size));
    }

    @GetMapping("/tasks/{taskId}")
    public Mono<TasksFacade.TaskView> getTask(@PathVariable String taskId) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.getTask(taskId, context.userId()));
    }

    @PatchMapping("/tasks/{taskId}")
    public Mono<TasksFacade.TaskView> updateTask(
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.updateTask(
                        taskId,
                        context.userId(),
                        request.title(),
                        request.description(),
                        request.status(),
                        request.assigneeUserId()
                ));
    }

    @PostMapping("/tasks/{taskId}/comments")
    public Mono<TasksFacade.TaskCommentView> addComment(
            @PathVariable String taskId,
            @Valid @RequestBody AddTaskCommentRequest request
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.addComment(taskId, context.userId(), request.body()));
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
