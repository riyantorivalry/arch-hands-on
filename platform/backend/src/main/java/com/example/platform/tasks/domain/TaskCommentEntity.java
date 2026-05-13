package com.example.platform.tasks.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_comments", indexes = {
        @Index(name = "idx_task_comments_task", columnList = "task_id,created_at")
})
public class TaskCommentEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "comment_id", nullable = false, length = 64)
    private String commentId;

    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    @Column(name = "author_user_id", nullable = false, length = 64)
    private String authorUserId;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    protected TaskCommentEntity() {
    }

    public TaskCommentEntity(String commentId, String taskId, String authorUserId, String body) {
        this.commentId = commentId;
        this.taskId = taskId;
        this.authorUserId = authorUserId;
        this.body = body;
    }

    public String getCommentId() {
        return commentId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public String getBody() {
        return body;
    }
}
