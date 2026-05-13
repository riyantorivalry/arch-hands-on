package com.example.platform.documents.infrastructure;

import com.example.platform.documents.domain.DocumentCommentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentCommentRepository extends JpaRepository<DocumentCommentEntity, String> {

    List<DocumentCommentEntity> findByDocumentIdOrderByCreatedAtAsc(String documentId);
}
