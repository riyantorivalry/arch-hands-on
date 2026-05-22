package com.example.platform.documents.api;

import com.example.platform.common.web.RequestContexts;
import com.example.platform.documents.application.DocumentsFacade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api")
public class DocumentsController {

    private final DocumentsFacade facade;

    public DocumentsController(DocumentsFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/workspaces/{workspaceId}/documents")
    public Mono<DocumentsFacade.DocumentView> createDocument(
            @PathVariable String workspaceId,
            @Valid @RequestBody CreateDocumentRequest request
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.createDocument(workspaceId, context.userId(), request.title(), request.content()));
    }

    @GetMapping("/workspaces/{workspaceId}/documents")
    public Mono<List<DocumentsFacade.DocumentView>> listDocuments(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.listDocuments(workspaceId, context.userId(), page, size));
    }

    @GetMapping("/workspaces/{workspaceId}/documents/search")
    public Mono<List<DocumentsFacade.DocumentView>> searchDocuments(
            @PathVariable String workspaceId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.searchDocuments(workspaceId, context.userId(), query, page, size));
    }

    @GetMapping("/v1/workspaces/{workspaceId}/documents/search")
    public Mono<List<DocumentsFacade.DocumentView>> searchDocumentsV1(
            @PathVariable String workspaceId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.searchDocumentsVersion("v1", workspaceId, context.userId(), query, page, size));
    }

    @GetMapping("/v2/workspaces/{workspaceId}/documents/search")
    public Mono<List<DocumentsFacade.DocumentView>> searchDocumentsV2(
            @PathVariable String workspaceId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.searchDocumentsVersion("v2", workspaceId, context.userId(), query, page, size));
    }

    @GetMapping("/v3/workspaces/{workspaceId}/documents/search")
    public Mono<List<DocumentsFacade.DocumentView>> searchDocumentsV3(
            @PathVariable String workspaceId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.searchDocumentsVersion("v3", workspaceId, context.userId(), query, page, size));
    }

    @GetMapping("/documents/{documentId}")
    public Mono<DocumentsFacade.DocumentView> getDocument(@PathVariable String documentId) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.getDocument(documentId, context.userId()));
    }

    @PatchMapping("/documents/{documentId}")
    public Mono<DocumentsFacade.DocumentView> updateDocument(
            @PathVariable String documentId,
            @Valid @RequestBody UpdateDocumentRequest request
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.updateDocument(documentId, context.userId(), request.title(), request.content(), request.status()));
    }

    @PostMapping("/documents/{documentId}/comments")
    public Mono<DocumentsFacade.DocumentCommentView> addComment(
            @PathVariable String documentId,
            @Valid @RequestBody AddCommentRequest request
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.addComment(documentId, context.userId(), request.body()));
    }

    public record CreateDocumentRequest(@NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 12000) String content) {
    }

    public record UpdateDocumentRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 12000) String content,
            String status
    ) {
    }

    public record AddCommentRequest(@NotBlank @Size(max = 4000) String body) {
    }
}
