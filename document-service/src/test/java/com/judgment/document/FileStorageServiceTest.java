package com.judgment.document;

import static org.junit.jupiter.api.Assertions.*;

import com.judgment.document.exception.ApiException;
import com.judgment.document.service.FileStorageService;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {
  @Test
  void rejectsNonPdf() throws Exception {
    FileStorageService storage =
        new FileStorageService(Files.createTempDirectory("docs").toString(), 1);
    MockMultipartFile file =
        new MockMultipartFile("file", "x.pdf", "application/pdf", "not-pdf".getBytes());
    assertThrows(ApiException.class, () -> storage.store(file, UUID.randomUUID()));
  }
}
