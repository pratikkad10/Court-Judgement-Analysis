package com.judgment.query;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.judgment.query.repository.*;
import com.judgment.query.service.ResearchService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

class ResearchOwnershipTest {
  @Test
  void cannotLoadOtherUsersSession() {
    ChatSessionRepository sessions = mock(ChatSessionRepository.class);
    when(sessions.findByIdAndOwnerId(any(), any())).thenReturn(Optional.empty());
    ResearchService service =
        new ResearchService(
            sessions,
            mock(ChatMessageRepository.class),
            mock(AnalysisRepository.class),
            mock(AuditLogRepository.class),
            new ObjectMapper(),
            "http://doc",
            "http://rag",
            "key");
    assertThrows(
        RuntimeException.class, () -> service.messages(UUID.randomUUID(), UUID.randomUUID()));
  }
}
