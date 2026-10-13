package com.judgment.document.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.judgment.document.exception.ApiException;
import com.judgment.document.repository.DocumentRepository;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;

class DocumentServiceOwnershipTest {
  @Test
  void cannotReadOtherUsersDocument() throws Exception {
    DocumentRepository docs = mock(DocumentRepository.class);
    when(docs.findByIdAndOwnerId(any(), any())).thenReturn(Optional.empty());
    DocumentService service =
        new DocumentService(
            docs,
            new FileStorageService(Files.createTempDirectory("storage").toString(), 1),
            500,
            "http://ingestion",
            "key");
    assertThrows(ApiException.class, () -> service.get(UUID.randomUUID(), UUID.randomUUID()));
  }
}
