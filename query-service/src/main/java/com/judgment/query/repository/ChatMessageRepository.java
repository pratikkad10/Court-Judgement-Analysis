package com.judgment.query.repository;

import com.judgment.query.entity.ChatMessage;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
  List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
