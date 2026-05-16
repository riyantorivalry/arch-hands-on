package com.example.platform.tasks.application;

import com.example.platform.common.audit.AuditLogger;
import com.example.platform.common.domain.DomainEventPublisher;
import com.example.platform.common.web.AuthorizationDeniedException;
import com.example.platform.identityaccess.application.AuthorizationService;
import com.example.platform.identityaccess.domain.MembershipEntity;
import com.example.platform.identityaccess.domain.MembershipRole;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TasksFacade {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 4000;

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
        String normalizedTitle = requireText("Task title", title, MAX_TITLE_LENGTH);
        String normalizedDescription = requireText("Task description", description, MAX_DESCRIPTION_LENGTH);
        String normalizedAssigneeUserId = blankToNull(assigneeUserId);
        if (normalizedAssigneeUserId != null) {
            requireActiveMembership(workspaceId, normalizedAssigneeUserId);
        }

        TaskEntity saved = taskRepository.save(new TaskEntity(
                "task-" + slugify(normalizedTitle) + "-" + UUID.randomUUID().toString().substring(0, 8),
                membership.getTenantId(),
                workspaceId,
                normalizedTitle,
                normalizedDescription,
                TaskStatus.TODO,
                normalizedAssigneeUserId,
                userId,
                userId
        ));
        auditLogger.logWrite("tasks", "create", "task", saved.getTaskId(), "SUCCESS");

        // Publish domain events
        domainEventPublisher.publish(new TaskCreatedEvent(
                membership.getTenantId(),
                saved.getTaskId(),
                workspaceId,
                normalizedTitle,
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

    @Transactional(readOnly = true)
    public List<TaskView> listTasks(String workspaceId) {
        return taskRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId).stream()
                .map(this::toTaskView)
                .toList();
    }

    @Transactional(readOnly = true)
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
        String normalizedTitle = requireText("Task title", title, MAX_TITLE_LENGTH);
        String normalizedDescription = requireText("Task description", description, MAX_DESCRIPTION_LENGTH);
        String normalizedAssigneeUserId = blankToNull(assigneeUserId);
        if (normalizedAssigneeUserId != null) {
            requireActiveMembership(task.getWorkspaceId(), normalizedAssigneeUserId);
        }

        TaskStatus nextStatus = parseStatus(status);
        boolean workspaceManager = isWorkspaceManager(membership);
        String previousStatus = task.getStatus().name();
        String previousAssignee = task.getAssigneeUserId();

        requireAssignmentRules(task, membership, normalizedAssigneeUserId, nextStatus, workspaceManager);
        requireTransition(task.getStatus(), nextStatus, workspaceManager);
        requireEditRules(task, membership, normalizedTitle, normalizedDescription, normalizedAssigneeUserId, nextStatus, workspaceManager);

        task.update(normalizedTitle, normalizedDescription, nextStatus, normalizedAssigneeUserId, userId);
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
        if (task.getStatus() == TaskStatus.CANCELED) {
            throw new IllegalStateException("Canceled tasks are locked for comments");
        }
        String normalizedBody = requireText("Task comment", body, MAX_DESCRIPTION_LENGTH);
        TaskCommentEntity saved = taskCommentRepository.save(new TaskCommentEntity(
                "task-comment-" + UUID.randomUUID(),
                taskId,
                userId,
                normalizedBody
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

    private MembershipEntity requireActiveMembership(String workspaceId, String userId) {
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
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "item" : slug;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private TaskStatus parseStatus(String status) {
        String normalized = requireText("Task status", status, 32);
        try {
            return TaskStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported task status: " + normalized);
        }
    }

    private void requireAssignmentRules(
            TaskEntity task,
            MembershipEntity membership,
            String nextAssigneeUserId,
            TaskStatus nextStatus,
            boolean workspaceManager
    ) {
        boolean assignmentChanged = !Objects.equals(task.getAssigneeUserId(), nextAssigneeUserId);
        if (assignmentChanged && !workspaceManager && !membership.getUserId().equals(task.getCreatedByUserId())) {
            throw new AuthorizationDeniedException("Only workspace managers or task creators can reassign tasks");
        }
        if (nextStatus != TaskStatus.TODO && nextStatus != TaskStatus.CANCELED && nextAssigneeUserId == null) {
            throw new IllegalStateException("Tasks must be assigned before they can leave TODO");
        }
        if (nextStatus == TaskStatus.DONE && !workspaceManager && !membership.getUserId().equals(nextAssigneeUserId)) {
            throw new AuthorizationDeniedException("Only the assignee or a workspace manager can complete a task");
        }
        if (nextStatus == TaskStatus.CANCELED && !workspaceManager) {
            throw new AuthorizationDeniedException("Only workspace managers can cancel tasks");
        }
    }

    private void requireTransition(TaskStatus currentStatus, TaskStatus nextStatus, boolean workspaceManager) {
        if (currentStatus == nextStatus) {
            return;
        }
        if (currentStatus == TaskStatus.CANCELED && !workspaceManager) {
            throw new AuthorizationDeniedException("Only workspace managers can reopen canceled tasks");
        }

        EnumSet<TaskStatus> allowed = switch (currentStatus) {
            case TODO -> EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED, TaskStatus.CANCELED);
            case IN_PROGRESS -> EnumSet.of(TaskStatus.TODO, TaskStatus.BLOCKED, TaskStatus.DONE, TaskStatus.CANCELED);
            case BLOCKED -> EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELED);
            case DONE -> EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELED);
            case CANCELED -> EnumSet.of(TaskStatus.TODO);
        };

        if (!allowed.contains(nextStatus)) {
            throw new IllegalStateException("Task status cannot move from " + currentStatus + " to " + nextStatus);
        }
    }

    private void requireEditRules(
            TaskEntity task,
            MembershipEntity membership,
            String nextTitle,
            String nextDescription,
            String nextAssigneeUserId,
            TaskStatus nextStatus,
            boolean workspaceManager
    ) {
        if (task.getStatus() == TaskStatus.CANCELED && !workspaceManager) {
            throw new AuthorizationDeniedException("Canceled tasks are locked to workspace managers");
        }
        boolean contentChanged = !Objects.equals(task.getTitle(), nextTitle)
                || !Objects.equals(task.getDescription(), nextDescription)
                || !Objects.equals(task.getAssigneeUserId(), nextAssigneeUserId);
        if (task.getStatus() == TaskStatus.DONE && contentChanged && !workspaceManager) {
            throw new AuthorizationDeniedException("Completed tasks can only be edited by workspace managers");
        }
        if (nextStatus == TaskStatus.DONE && nextDescription.length() < 20) {
            throw new IllegalStateException("Completed tasks require a description with at least 20 characters");
        }
    }

    private String requireText(String fieldName, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    private boolean isWorkspaceManager(MembershipEntity membership) {
        return membership.getRole() == MembershipRole.OWNER || membership.getRole() == MembershipRole.ADMIN;
    }
}
