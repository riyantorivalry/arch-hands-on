package com.example.platform.tasks.application;

import com.example.platform.common.audit.AuditLogger;
import com.example.platform.common.domain.DomainEventPublisher;
import com.example.platform.identityaccess.application.AuthorizationService;
import com.example.platform.identityaccess.domain.MembershipStatus;
import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import com.example.platform.tasks.domain.TaskAssignedEvent;
import com.example.platform.tasks.domain.TaskCommentEntity;
import com.example.platform.tasks.domain.TaskCreatedEvent;
import com.example.platform.tasks.domain.TaskEntity;
import com.example.platform.tasks.domain.TaskStatus;
import com.example.platform.tasks.domain.TaskStatusChangedEvent;
import com.example.platform.tasks.infrastructure.TaskCommentRepository;
import com.example.platform.tasks.infrastructure.TaskRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TasksFacade {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final MembershipRepository membershipRepository;
    private final AuditLogger auditLogger;
    private final AuthorizationService authorizationService;
    private final DomainEventPublisher domainEventPublisher;

    public TasksFacade(
            TaskRepository taskRepository,
            TaskCommentRepository taskCommentRepository,
            MembershipRepository membershipRepository,
            AuditLogger auditLogger,
            AuthorizationService authorizationService,
            DomainEventPublisher domainEventPublisher
    ) {
        this.taskRepository = taskRepository;
        this.taskCommentRepository = taskCommentRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogger = auditLogger;
        this.authorizationService = authorizationService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public TaskView createTask(String workspaceId, String userId, String title, String description, String assigneeUserId) {
        var membership = requireActiveMembership(workspaceId, userId);
        if (assigneeUserId != null && !assigneeUserId.isBlank()) {
            requireActiveMembership(workspaceId, assigneeUserId);
        }

        TaskEntity saved = taskRepository.save(new TaskEntity(
                "task-" + slugify(title) + "-" + UUID.randomUUID().toString().substring(0, 8),
                membership.getTenantId(),
                workspaceId,
                title,
                description,
                TaskStatus.TODO,
                blankToNull(assigneeUserId),
                userId,
                userId
        ));
        auditLogger.logWrite("tasks", "create", "task", saved.getTaskId(), "SUCCESS");

        // Publish domain events
        domainEventPublisher.publish(new TaskCreatedEvent(
                membership.getTenantId(),
                saved.getTaskId(),
                workspaceId,
                title,
                userId
        ));

        if (saved.getAssigneeUserId() != null) {
            domainEventPublisher.publish(new TaskAssignedEvent(
                    membership.getTenantId(),
                    saved.getTaskId(),
                    workspaceId,
                    saved.getAssigneeUserId()
            ));
        }

        return toTaskView(saved);
    }

    public List<TaskView> listTasks(String workspaceId) {
        return taskRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId).stream()
                .map(this::toTaskView)
                .toList();
    }

    public TaskView getTask(String taskId) {
        return taskRepository.findById(taskId)
                .map(this::toTaskView)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    @Transactional
    public TaskView updateTask(String taskId, String userId, String title, String description, String status, String assigneeUserId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        var membership = requireActiveMembership(task.getWorkspaceId(), userId);
        authorizationService.requireOwnerOrAdminOrAssignee(membership, task.getCreatedByUserId(), task.getAssigneeUserId());
        if (assigneeUserId != null && !assigneeUserId.isBlank()) {
            requireActiveMembership(task.getWorkspaceId(), assigneeUserId);
        }

        TaskStatus nextStatus = TaskStatus.valueOf(status);
        String previousStatus = task.getStatus().name();
        String previousAssignee = task.getAssigneeUserId();

        task.update(title, description, nextStatus, blankToNull(assigneeUserId), userId);
        auditLogger.logWrite("tasks", "update", "task", task.getTaskId(), "SUCCESS");

        // Publish domain events
        if (!previousStatus.equals(nextStatus.name())) {
            domainEventPublisher.publish(new TaskStatusChangedEvent(
                    membership.getTenantId(),
                    task.getTaskId(),
                    task.getWorkspaceId(),
                    previousStatus,
                    nextStatus.name()
            ));
        }

        if (!java.util.Objects.equals(previousAssignee, task.getAssigneeUserId()) && task.getAssigneeUserId() != null) {
            domainEventPublisher.publish(new TaskAssignedEvent(
                    membership.getTenantId(),
                    task.getTaskId(),
                    task.getWorkspaceId(),
                    task.getAssigneeUserId()
            ));
        }

        return toTaskView(task);
    }

    @Transactional
    public TaskCommentView addComment(String taskId, String userId, String body) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        requireActiveMembership(task.getWorkspaceId(), userId);
        TaskCommentEntity saved = taskCommentRepository.save(new TaskCommentEntity(
                "task-comment-" + UUID.randomUUID(),
                taskId,
                userId,
                body
        ));
        auditLogger.logWrite("tasks", "comment", "task", taskId, "SUCCESS");
        return new TaskCommentView(saved.getCommentId(), saved.getTaskId(), saved.getAuthorUserId(), saved.getBody());
    }

    public record TaskView(
            String taskId,
            String workspaceId,
            String title,
            String description,
            String status,
            String assigneeUserId,
            String createdByUserId
    ) {
    }

    public record TaskCommentView(String commentId, String taskId, String authorUserId, String body) {
    }

    private com.example.platform.identityaccess.domain.MembershipEntity requireActiveMembership(String workspaceId, String userId) {
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of workspace " + workspaceId));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Membership is not active for user " + userId);
        }
        return membership;
    }

    private TaskView toTaskView(TaskEntity task) {
        return new TaskView(
                task.getTaskId(),
                task.getWorkspaceId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getAssigneeUserId(),
                task.getCreatedByUserId()
        );
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
