package com.example.platform.documents.infrastructure;

import com.example.platform.documents.domain.DocumentCommentEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DocumentCommentRepository extends ReactiveCrudRepository<DocumentCommentEntity, String> {

    Flux<DocumentCommentEntity> findByDocumentIdOrderByCreatedAtAsc(String documentId);
}
