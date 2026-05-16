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

@Validated
@RestController
@RequestMapping("/api")
public class DocumentsController {

    private final DocumentsFacade facade;

    public DocumentsController(DocumentsFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/workspaces/{workspaceId}/documents")
    public DocumentsFacade.DocumentView createDocument(
            @PathVariable String workspaceId,
            @Valid @RequestBody CreateDocumentRequest request
    ) {
        return facade.createDocument(workspaceId, RequestContexts.current().userId(), request.title(), request.content());
    }

    @GetMapping("/workspaces/{workspaceId}/documents")
    public List<DocumentsFacade.DocumentView> listDocuments(@PathVariable String workspaceId) {
        return facade.listDocuments(workspaceId);
    }

    @GetMapping("/workspaces/{workspaceId}/documents/search")
    public List<DocumentsFacade.DocumentView> searchDocuments(
            @PathVariable String workspaceId,
            @RequestParam String query
    ) {
        return facade.searchDocuments(workspaceId, query);
    }

    @GetMapping("/documents/{documentId}")
    public DocumentsFacade.DocumentView getDocument(@PathVariable String documentId) {
        return facade.getDocument(documentId);
    }

    @PatchMapping("/documents/{documentId}")
    public DocumentsFacade.DocumentView updateDocument(
            @PathVariable String documentId,
            @Valid @RequestBody UpdateDocumentRequest request
    ) {
        return facade.updateDocument(documentId, RequestContexts.current().userId(), request.title(), request.content(), request.status());
    }

    @PostMapping("/documents/{documentId}/comments")
    public DocumentsFacade.DocumentCommentView addComment(
            @PathVariable String documentId,
            @Valid @RequestBody AddCommentRequest request
    ) {
        return facade.addComment(documentId, RequestContexts.current().userId(), request.body());
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
