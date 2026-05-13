package com.example.platform.documents.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_comments", indexes = {
        @Index(name = "idx_document_comments_document", columnList = "document_id,created_at")
})
public class DocumentCommentEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "comment_id", nullable = false, length = 64)
    private String commentId;

    @Column(name = "document_id", nullable = false, length = 64)
    private String documentId;

    @Column(name = "author_user_id", nullable = false, length = 64)
    private String authorUserId;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    protected DocumentCommentEntity() {
    }

    public DocumentCommentEntity(String commentId, String documentId, String authorUserId, String body) {
        this.commentId = commentId;
        this.documentId = documentId;
        this.authorUserId = authorUserId;
        this.body = body;
    }

    public String getCommentId() {
        return commentId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public String getBody() {
        return body;
    }
}
