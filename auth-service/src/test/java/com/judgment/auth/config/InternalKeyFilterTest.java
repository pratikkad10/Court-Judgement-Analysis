package com.judgment.auth.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;

class InternalKeyFilterTest {
  @Test
  void rejectsRequestWithoutInternalKey() throws Exception {
    InternalKeyFilter filter = new InternalKeyFilter("key");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    assertEquals(401, response.getStatus());
  }
}
