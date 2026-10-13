package com.judgment.query.repository;

import com.judgment.query.entity.ChatSession;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
  List<ChatSession> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

  Optional<ChatSession> findByIdAndOwnerId(UUID id, UUID ownerId);
}
