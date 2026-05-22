package com.example.platform.documents.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("document_comments")
public class DocumentCommentEntity extends AbstractAuditableEntity {

    @Id
    @Column("comment_id")
    private String commentId;

    @Column("document_id")
    private String documentId;

    @Column("author_user_id")
    private String authorUserId;

    @Column("body")
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

    @Override
    public Object getId() {
        return commentId;
    }
}
