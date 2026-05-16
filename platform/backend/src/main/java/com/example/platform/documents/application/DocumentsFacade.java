package com.example.platform.documents.application;

import com.example.platform.analytics.application.AnalyticsService;
import com.example.platform.common.audit.AuditLogger;
import com.example.platform.common.domain.DomainEventPublisher;
import com.example.platform.identityaccess.application.AuthorizationService;
import com.example.platform.documents.domain.DocumentCommentEntity;
import com.example.platform.documents.domain.DocumentCreatedEvent;
import com.example.platform.documents.domain.DocumentEntity;
import com.example.platform.documents.domain.DocumentStatus;
import com.example.platform.documents.domain.DocumentUpdatedEvent;
import com.example.platform.documents.infrastructure.DocumentCommentRepository;
import com.example.platform.documents.infrastructure.DocumentRepository;
import com.example.platform.documents.infrastructure.DocumentSearchRepository;
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
    private final DomainEventPublisher domainEventPublisher;
    private final AnalyticsService analyticsService;
    private final DocumentSearchRepository documentSearchRepository;

    public DocumentsFacade(
            DocumentRepository documentRepository,
            DocumentCommentRepository documentCommentRepository,
            MembershipRepository membershipRepository,
            AuditLogger auditLogger,
            AuthorizationService authorizationService,
            DomainEventPublisher domainEventPublisher,
            AnalyticsService analyticsService,
            DocumentSearchRepository documentSearchRepository
    ) {
        this.documentRepository = documentRepository;
        this.documentCommentRepository = documentCommentRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogger = auditLogger;
        this.authorizationService = authorizationService;
        this.domainEventPublisher = domainEventPublisher;
        this.analyticsService = analyticsService;
        this.documentSearchRepository = documentSearchRepository;
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

        // Index in OpenSearch for full-text search
        indexDocument(saved, membership.getTenantId());

        // Publish domain event
        domainEventPublisher.publish(new DocumentCreatedEvent(
                membership.getTenantId(),
                saved.getDocumentId(),
                workspaceId,
                title,
                userId
        ));

        return toDocumentView(saved);
    }

    public List<DocumentView> listDocuments(String workspaceId) {
        return documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId).stream()
                .map(this::toDocumentView)
                .toList();
    }

    public List<DocumentView> searchDocuments(String workspaceId, String query) {
        // Use OpenSearch for full-text search
        List<DocumentView> results = documentSearchRepository
                .findByWorkspaceIdAndTitleContainsOrContentContains(workspaceId, query, query)
                .stream()
                .map(doc -> new DocumentView(
                        doc.getDocumentId(),
                        doc.getWorkspaceId(),
                        doc.getTitle(),
                        doc.getContent(),
                        doc.getStatus(),
                        doc.getCreatedByUserId(),
                        doc.getLastModifiedByUserId()
                ))
                .toList();

        // Track search analytics
        analyticsService.trackSearch(query, results.size());

        return results;
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

        // Update index in OpenSearch
        indexDocument(document, membership.getTenantId());

        // Publish domain event
        domainEventPublisher.publish(new DocumentUpdatedEvent(
                membership.getTenantId(),
                document.getDocumentId(),
                document.getWorkspaceId(),
                title,
                userId
        ));

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

    public record DocumentView(
            String documentId,
            String workspaceId,
            String title,
            String content,
            String status,
            String createdByUserId,
            String lastModifiedByUserId
    ) {
    }

    public record DocumentCommentView(String commentId, String documentId, String authorUserId, String body) {
    }

    private DocumentView toDocumentView(DocumentEntity document) {
        return new DocumentView(
                document.getDocumentId(),
                document.getWorkspaceId(),
                document.getTitle(),
                document.getContent(),
                document.getStatus().name(),
                document.getCreatedByUserId(),
                document.getLastModifiedByUserId()
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

    private void indexDocument(DocumentEntity document, String tenantId) {
        try {
            var searchDoc = new com.example.platform.documents.domain.DocumentSearchDocument(
                    document.getDocumentId(),
                    tenantId,
                    document.getWorkspaceId(),
                    document.getTitle(),
                    document.getContent(),
                    document.getStatus().name(),
                    document.getCreatedByUserId(),
                    document.getLastModifiedByUserId(),
                    document.getCreatedAt(),
                    document.getUpdatedAt()
            );
            documentSearchRepository.save(searchDoc);
        } catch (Exception e) {
            auditLogger.logWrite("documents", "search_index", "document", document.getDocumentId(), "FAILED: " + e.getMessage());
        }
    }
}
