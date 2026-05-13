package com.example.platform.documents.application;

import com.example.platform.common.audit.AuditLogger;
import com.example.platform.identityaccess.application.AuthorizationService;
import com.example.platform.documents.domain.DocumentCommentEntity;
import com.example.platform.documents.domain.DocumentEntity;
import com.example.platform.documents.domain.DocumentStatus;
import com.example.platform.documents.infrastructure.DocumentCommentRepository;
import com.example.platform.documents.infrastructure.DocumentRepository;
import com.example.platform.identityaccess.domain.MembershipStatus;
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
    private final AuditLogger auditLogger;
    private final AuthorizationService authorizationService;

    public DocumentsFacade(
            DocumentRepository documentRepository,
            DocumentCommentRepository documentCommentRepository,
            MembershipRepository membershipRepository,
            AuditLogger auditLogger,
            AuthorizationService authorizationService
    ) {
        this.documentRepository = documentRepository;
        this.documentCommentRepository = documentCommentRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogger = auditLogger;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public DocumentView createDocument(String workspaceId, String userId, String title, String content) {
        var membership = requireActiveMembership(workspaceId, userId);
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
        auditLogger.logWrite("documents", "create", "document", saved.getDocumentId(), "SUCCESS");
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
        var membership = requireActiveMembership(document.getWorkspaceId(), userId);
        authorizationService.requireOwnerOrAdminOrResourceOwner(membership, document.getCreatedByUserId());
        document.update(title, content, userId);
        auditLogger.logWrite("documents", "update", "document", document.getDocumentId(), "SUCCESS");
        return toDocumentView(document);
    }

    @Transactional
    public DocumentCommentView addComment(String documentId, String userId, String body) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        requireActiveMembership(document.getWorkspaceId(), userId);
        DocumentCommentEntity saved = documentCommentRepository.save(new DocumentCommentEntity(
                "comment-" + UUID.randomUUID(),
                documentId,
                userId,
                body
        ));
        auditLogger.logWrite("documents", "comment", "document", documentId, "SUCCESS");
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

    private com.example.platform.identityaccess.domain.MembershipEntity requireActiveMembership(String workspaceId, String userId) {
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of workspace " + workspaceId));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Membership is not active for user " + userId);
        }
        return membership;
    }
}
