package com.judgment.document.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.judgment.document.dto.DocumentDtos.*;
import com.judgment.document.service.DocumentService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DocumentControllerTest {
  @Test
  void listsDocumentsInEnvelope() throws Exception {
    DocumentService service = mock(DocumentService.class);
    UUID user = UUID.randomUUID();
    when(service.list(user))
        .thenReturn(
            List.of(
                new DocumentView(
                    UUID.randomUUID(),
                    "Judgment",
                    "a.pdf",
                    2,
                    com.judgment.document.entity.DocumentStatus.READY,
                    null,
                    null,
                    null,
                    Instant.now())));
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new DocumentController(service)).build();
    mvc.perform(get("/api/documents").header("X-User-Id", user))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].title").value("Judgment"));
  }
}
