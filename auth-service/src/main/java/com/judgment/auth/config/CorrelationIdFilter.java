package com.judgment.auth.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Places a propagated correlation ID in logging context for each servlet request. */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String id = request.getHeader("X-Correlation-Id");
    if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
    response.setHeader("X-Correlation-Id", id);
    try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", id)) {
      chain.doFilter(request, response);
    }
  }
}
