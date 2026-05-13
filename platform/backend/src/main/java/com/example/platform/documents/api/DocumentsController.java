package com.example.platform.documents.api;

import com.example.platform.common.web.RequestContexts;
import com.example.platform.documents.application.DocumentsFacade;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
            @RequestBody CreateDocumentRequest request
    ) {
        return facade.createDocument(workspaceId, request.title());
    }

    @GetMapping("/workspaces/{workspaceId}/documents")
    public List<DocumentsFacade.DocumentView> listDocuments(@PathVariable String workspaceId) {
        return facade.listDocuments(workspaceId);
    }

    @GetMapping("/documents/{documentId}")
    public DocumentsFacade.DocumentView getDocument(@PathVariable String documentId) {
        return facade.getDocument(documentId);
    }

    @PatchMapping("/documents/{documentId}")
    public DocumentsFacade.DocumentView updateDocument(@PathVariable String documentId) {
        return facade.updateDocument(documentId);
    }

    @PostMapping("/documents/{documentId}/comments")
    public DocumentsFacade.DocumentCommentView addComment(
            @PathVariable String documentId,
            @RequestBody AddCommentRequest request
    ) {
        return facade.addComment(documentId, RequestContexts.current().userId(), request.body());
    }

    public record CreateDocumentRequest(@NotBlank String title) {
    }

    public record AddCommentRequest(@NotBlank String body) {
    }
}
