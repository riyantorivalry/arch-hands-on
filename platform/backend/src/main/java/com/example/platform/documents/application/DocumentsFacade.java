package com.example.platform.documents.application;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DocumentsFacade {

    public DocumentView createDocument(String workspaceId, String title) {
        return new DocumentView("document-" + title.toLowerCase().replace(" ", "-"), workspaceId, title, "DRAFT");
    }

    public List<DocumentView> listDocuments(String workspaceId) {
        return List.of(new DocumentView("document-platform-plan", workspaceId, "Platform Plan", "ACTIVE"));
    }

    public DocumentView getDocument(String documentId) {
        return new DocumentView(documentId, "workspace-dev", "Platform Plan", "ACTIVE");
    }

    public DocumentView updateDocument(String documentId) {
        return new DocumentView(documentId, "workspace-dev", "Platform Plan", "ACTIVE");
    }

    public DocumentCommentView addComment(String documentId, String userId, String body) {
        return new DocumentCommentView("comment-1", documentId, userId, body);
    }

    public record DocumentView(String documentId, String workspaceId, String title, String status) {
    }

    public record DocumentCommentView(String commentId, String documentId, String authorUserId, String body) {
    }
}
