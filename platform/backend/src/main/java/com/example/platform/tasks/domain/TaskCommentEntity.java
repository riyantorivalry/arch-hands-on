package com.example.platform.tasks.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("task_comments")
public class TaskCommentEntity extends AbstractAuditableEntity {

    @Id
    @Column("comment_id")
    private String commentId;

    @Column("task_id")
    private String taskId;

    @Column("author_user_id")
    private String authorUserId;

    @Column("body")
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

    @Override
    public Object getId() {
        return commentId;
    }
}
