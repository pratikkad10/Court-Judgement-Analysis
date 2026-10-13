package com.judgment.gateway.filter;

import static org.junit.jupiter.api.Assertions.*;

import com.judgment.gateway.util.JwtVerifier;
import org.junit.jupiter.api.*;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class RequestSecurityFilterTest {
  @Test
  void rejectsPrivateRouteWithoutBearerToken() {
    RequestSecurityFilter filter =
        new RequestSecurityFilter(
            new JwtVerifier("12345678901234567890123456789012"), "internal", 100, 10);
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/documents").build());
    filter.filter(exchange, e -> Mono.empty()).block();
    assertEquals(401, exchange.getResponse().getStatusCode().value());
  }
}
