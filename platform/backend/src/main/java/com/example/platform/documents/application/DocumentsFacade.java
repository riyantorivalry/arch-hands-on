package com.example.platform.documents.application;

import com.example.platform.documents.domain.DocumentCommentEntity;
import com.example.platform.documents.domain.DocumentEntity;
import com.example.platform.documents.domain.DocumentStatus;
import com.example.platform.documents.infrastructure.DocumentCommentRepository;
import com.example.platform.documents.infrastructure.DocumentRepository;
import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentsFacade {

    private final DocumentRepository documentRepository;
    private final DocumentCommentRepository documentCommentRepository;
    private final MembershipRepository membershipRepository;

    public DocumentsFacade(
            DocumentRepository documentRepository,
            DocumentCommentRepository documentCommentRepository,
            MembershipRepository membershipRepository
    ) {
        this.documentRepository = documentRepository;
        this.documentCommentRepository = documentCommentRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public DocumentView createDocument(String workspaceId, String userId, String title, String content) {
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of workspace " + workspaceId));
        DocumentEntity saved = documentRepository.save(new DocumentEntity(
                "document-" + slugify(title) + "-" + UUID.randomUUID().toString().substring(0, 8),
                membership.getTenantId(),
                workspaceId,
                title,
                content,
                DocumentStatus.DRAFT,
                userId,
                userId
        ));
        return toDocumentView(saved);
    }

    public List<DocumentView> listDocuments(String workspaceId) {
        return documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId).stream()
                .map(this::toDocumentView)
                .toList();
    }

    public DocumentView getDocument(String documentId) {
        return documentRepository.findById(documentId)
                .map(this::toDocumentView)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    }

    @Transactional
    public DocumentView updateDocument(String documentId, String userId, String title, String content) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        membershipRepository.findByWorkspaceIdAndUserId(document.getWorkspaceId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of workspace " + document.getWorkspaceId()));
        document.update(title, content, userId);
        return toDocumentView(document);
    }

    @Transactional
    public DocumentCommentView addComment(String documentId, String userId, String body) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        membershipRepository.findByWorkspaceIdAndUserId(document.getWorkspaceId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of workspace " + document.getWorkspaceId()));
        DocumentCommentEntity saved = documentCommentRepository.save(new DocumentCommentEntity(
                "comment-" + UUID.randomUUID(),
                documentId,
                userId,
                body
        ));
        return new DocumentCommentView(saved.getCommentId(), saved.getDocumentId(), saved.getAuthorUserId(), saved.getBody());
    }

    public record DocumentView(String documentId, String workspaceId, String title, String content, String status) {
    }

    public record DocumentCommentView(String commentId, String documentId, String authorUserId, String body) {
    }

    private DocumentView toDocumentView(DocumentEntity document) {
        return new DocumentView(
                document.getDocumentId(),
                document.getWorkspaceId(),
                document.getTitle(),
                document.getContent(),
                document.getStatus().name()
        );
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
