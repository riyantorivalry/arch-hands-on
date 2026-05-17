package com.example.platform.documents.application;

import com.example.platform.analytics.application.AnalyticsService;
import com.example.platform.common.audit.AuditLogger;
import com.example.platform.common.domain.DomainEventPublisher;
import com.example.platform.common.web.AuthorizationDeniedException;
import com.example.platform.identityaccess.application.AuthorizationService;
import com.example.platform.documents.domain.DocumentCommentEntity;
import com.example.platform.documents.domain.DocumentCreatedEvent;
import com.example.platform.documents.domain.DocumentEntity;
import com.example.platform.documents.domain.DocumentStatus;
import com.example.platform.documents.domain.DocumentUpdatedEvent;
import com.example.platform.documents.infrastructure.DocumentCommentRepository;
import com.example.platform.documents.infrastructure.DocumentRepository;
import com.example.platform.documents.infrastructure.DocumentSearchRepository;
import com.example.platform.identityaccess.domain.MembershipEntity;
import com.example.platform.identityaccess.domain.MembershipRole;
import com.example.platform.identityaccess.domain.MembershipStatus;
import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import java.text.Normalizer;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DocumentsFacade {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 12000;
    private static final int MIN_REVIEW_CONTENT_LENGTH = 20;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final DocumentRepository documentRepository;
    private final DocumentCommentRepository documentCommentRepository;
    private final MembershipRepository membershipRepository;
    private final AuditLogger auditLogger;
    private final AuthorizationService authorizationService;
    private final DomainEventPublisher domainEventPublisher;
    private final AnalyticsService analyticsService;
    private final DocumentSearchRepository documentSearchRepository;
    private final TransactionTemplate transactionTemplate;

    public DocumentsFacade(
            DocumentRepository documentRepository,
            DocumentCommentRepository documentCommentRepository,
            MembershipRepository membershipRepository,
            AuditLogger auditLogger,
            AuthorizationService authorizationService,
            DomainEventPublisher domainEventPublisher,
            AnalyticsService analyticsService,
            DocumentSearchRepository documentSearchRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.documentRepository = documentRepository;
        this.documentCommentRepository = documentCommentRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogger = auditLogger;
        this.authorizationService = authorizationService;
        this.domainEventPublisher = domainEventPublisher;
        this.analyticsService = analyticsService;
        this.documentSearchRepository = documentSearchRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setReadOnly(true);
    }

    @Transactional
    public DocumentView createDocument(String workspaceId, String userId, String title, String content) {
        var membership = requireActiveMembership(workspaceId, userId);
        String normalizedTitle = requireText("Document title", title, MAX_TITLE_LENGTH);
        String normalizedContent = requireText("Document content", content, MAX_CONTENT_LENGTH);
        DocumentEntity saved = documentRepository.save(new DocumentEntity(
                "document-" + slugify(normalizedTitle) + "-" + UUID.randomUUID().toString().substring(0, 8),
                membership.getTenantId(),
                workspaceId,
                normalizedTitle,
                normalizedContent,
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
                normalizedTitle,
                userId
        ));

        return toDocumentView(saved);
    }

    @Transactional(readOnly = true)
    public List<DocumentView> listDocuments(String workspaceId) {
        return listDocuments(workspaceId, null, 0, DEFAULT_PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public List<DocumentView> listDocuments(String workspaceId, String userId, int page, int size) {
        if (userId != null) {
            requireActiveMembership(workspaceId, userId);
        }
        return documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId, pageRequest(page, size)).stream()
                .map(this::toDocumentView)
                .toList();
    }

    public List<DocumentView> searchDocuments(String workspaceId, String query) {
        return searchDocuments(workspaceId, null, query, 0, DEFAULT_PAGE_SIZE);
    }

    public List<DocumentView> searchDocuments(String workspaceId, String userId, String query, int page, int size) {
        if (userId != null) {
            requireActiveMembership(workspaceId, userId);
        }
        Pageable pageable = pageRequest(page, size);
        List<DocumentView> results;
        try {
            results = transactionTemplate.execute(status -> searchDocumentsWithOpenSearch(workspaceId, query, pageable));
        } catch (RuntimeException exception) {
            auditLogger.logWrite("documents", "search", "workspace", workspaceId, "OPENSEARCH_FALLBACK: " + exception.getMessage());
            results = transactionTemplate.execute(status -> searchDocumentsWithPostgres(workspaceId, query, pageable));
        }

        analyticsService.trackSearch(query, results.size());
        return results;
    }

    private List<DocumentView> searchDocumentsWithOpenSearch(String workspaceId, String query, Pageable pageable) {
        return documentSearchRepository
                .findByWorkspaceIdAndTitleContainsOrContentContains(workspaceId, query, query, pageable)
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
    }

    private List<DocumentView> searchDocumentsWithPostgres(String workspaceId, String query, Pageable pageable) {
        return documentRepository.searchByWorkspaceIdAndQuery(workspaceId, query, pageable).stream()
                .map(this::toDocumentView)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentView getDocument(String documentId) {
        return getDocument(documentId, null);
    }

    @Transactional(readOnly = true)
    public DocumentView getDocument(String documentId, String userId) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        if (userId != null) {
            requireActiveMembership(document.getWorkspaceId(), userId);
        }
        return toDocumentView(document);
    }

    @Transactional
    public DocumentView updateDocument(String documentId, String userId, String title, String content, String status) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        var membership = requireActiveMembership(document.getWorkspaceId(), userId);
        authorizationService.requireOwnerOrAdminOrResourceOwner(membership, document.getCreatedByUserId());
        String normalizedTitle = requireText("Document title", title, MAX_TITLE_LENGTH);
        String normalizedContent = requireText("Document content", content, MAX_CONTENT_LENGTH);
        DocumentStatus nextStatus = parseStatus(status, document.getStatus());
        boolean workspaceManager = isWorkspaceManager(membership);

        requireDocumentTransition(document, nextStatus, workspaceManager);
        requireDocumentEditRules(document, normalizedTitle, normalizedContent, nextStatus, workspaceManager);

        document.update(normalizedTitle, normalizedContent, nextStatus, userId);
        auditLogger.logWrite("documents", "update", "document", document.getDocumentId(), "SUCCESS");

        // Update index in OpenSearch
        indexDocument(document, membership.getTenantId());

        // Publish domain event
        domainEventPublisher.publish(new DocumentUpdatedEvent(
                membership.getTenantId(),
                document.getDocumentId(),
                document.getWorkspaceId(),
                normalizedTitle,
                userId
        ));

        return toDocumentView(document);
    }

    @Transactional
    public DocumentCommentView addComment(String documentId, String userId, String body) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        requireActiveMembership(document.getWorkspaceId(), userId);
        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new IllegalStateException("Archived documents are locked for comments");
        }
        String normalizedBody = requireText("Document comment", body, 4000);
        DocumentCommentEntity saved = documentCommentRepository.save(new DocumentCommentEntity(
                "comment-" + UUID.randomUUID(),
                documentId,
                userId,
                normalizedBody
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
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "item" : slug;
    }

    private MembershipEntity requireActiveMembership(String workspaceId, String userId) {
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AuthorizationDeniedException("User is not a member of workspace " + workspaceId));
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

    private DocumentStatus parseStatus(String status, DocumentStatus fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        String normalized = status.trim();
        try {
            return DocumentStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported document status: " + normalized);
        }
    }

    private void requireDocumentTransition(DocumentEntity document, DocumentStatus nextStatus, boolean workspaceManager) {
        DocumentStatus currentStatus = document.getStatus();
        if (currentStatus == nextStatus) {
            return;
        }
        if ((nextStatus == DocumentStatus.ACTIVE || nextStatus == DocumentStatus.ARCHIVED) && !workspaceManager) {
            throw new AuthorizationDeniedException("Only workspace managers can publish or archive documents");
        }
        if (currentStatus == DocumentStatus.ARCHIVED && !workspaceManager) {
            throw new AuthorizationDeniedException("Only workspace managers can restore archived documents");
        }

        EnumSet<DocumentStatus> allowed = switch (currentStatus) {
            case DRAFT -> EnumSet.of(DocumentStatus.IN_REVIEW, DocumentStatus.ACTIVE, DocumentStatus.ARCHIVED);
            case IN_REVIEW -> EnumSet.of(DocumentStatus.DRAFT, DocumentStatus.ACTIVE, DocumentStatus.ARCHIVED);
            case ACTIVE -> EnumSet.of(DocumentStatus.DRAFT, DocumentStatus.ARCHIVED);
            case ARCHIVED -> EnumSet.of(DocumentStatus.DRAFT);
        };

        if (!allowed.contains(nextStatus)) {
            throw new IllegalStateException("Document status cannot move from " + currentStatus + " to " + nextStatus);
        }
    }

    private void requireDocumentEditRules(
            DocumentEntity document,
            String nextTitle,
            String nextContent,
            DocumentStatus nextStatus,
            boolean workspaceManager
    ) {
        boolean contentChanged = !Objects.equals(document.getTitle(), nextTitle)
                || !Objects.equals(document.getContent(), nextContent);
        if (document.getStatus() == DocumentStatus.ARCHIVED && contentChanged) {
            throw new IllegalStateException("Archived documents must be restored to DRAFT before editing content");
        }
        if (document.getStatus() == DocumentStatus.ACTIVE && contentChanged && nextStatus == DocumentStatus.ACTIVE) {
            throw new IllegalStateException("Active documents must move back to DRAFT before content changes");
        }
        if ((nextStatus == DocumentStatus.IN_REVIEW || nextStatus == DocumentStatus.ACTIVE)
                && nextContent.length() < MIN_REVIEW_CONTENT_LENGTH) {
            throw new IllegalStateException("Documents submitted for review or publishing require at least "
                    + MIN_REVIEW_CONTENT_LENGTH + " characters");
        }
        if (nextStatus == DocumentStatus.ACTIVE && !workspaceManager) {
            throw new AuthorizationDeniedException("Only workspace managers can publish documents");
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

    private Pageable pageRequest(int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize);
    }
}
