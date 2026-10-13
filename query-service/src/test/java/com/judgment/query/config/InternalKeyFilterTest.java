package com.judgment.query.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;

class InternalKeyFilterTest {
  @Test
  void rejectsDirectRequest() throws Exception {
    InternalKeyFilter filter = new InternalKeyFilter("key");
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(
        new MockHttpServletRequest("GET", "/api/chat/sessions"), response, new MockFilterChain());
    assertEquals(401, response.getStatus());
  }
}
