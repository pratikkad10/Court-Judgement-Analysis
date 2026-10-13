package com.judgment.query.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.judgment.query.dto.QueryDtos.*;
import com.judgment.query.service.ResearchService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class QueryControllerTest {
  @Test
  void listsOnlyRequestedUsersSessions() throws Exception {
    ResearchService service = mock(ResearchService.class);
    UUID user = UUID.randomUUID();
    when(service.sessions(user))
        .thenReturn(
            List.of(new SessionView(UUID.randomUUID(), "Research", Instant.now(), Instant.now())));
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new QueryController(service)).build();
    mvc.perform(get("/api/chat/sessions").header("X-User-Id", user))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].title").value("Research"));
  }
}
