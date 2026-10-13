package com.judgment.query.repository;

import com.judgment.query.entity.Analysis;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {
  List<Analysis> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
