package com.judgment.document.repository;

import com.judgment.document.entity.JudgmentDocument;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<JudgmentDocument, UUID> {
  List<JudgmentDocument> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

  Optional<JudgmentDocument> findByIdAndOwnerId(UUID id, UUID ownerId);

  boolean existsByOwnerIdAndFileHash(UUID ownerId, String fileHash);
}
