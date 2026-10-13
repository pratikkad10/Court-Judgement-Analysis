package com.judgment.gateway.filter;

import com.judgment.gateway.util.JwtVerifier;
import io.jsonwebtoken.Claims;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Adds trusted identity headers and applies a small in-memory edge rate limiter. */
@Component
public class RequestSecurityFilter implements GlobalFilter, Ordered {
  private final JwtVerifier verifier;
  private final String internalKey;
  private final int defaultLimit, aiLimit;
  private final Map<String, Window> limits = new ConcurrentHashMap<>();
  private static final Set<String> PUBLIC =
      Set.of("/api/auth/register", "/api/auth/login", "/api/auth/refresh");

  public RequestSecurityFilter(
      JwtVerifier verifier,
      @Value("${app.internal-key}") String internalKey,
      @Value("${app.default-limit}") int defaultLimit,
      @Value("${app.ai-limit}") int aiLimit) {
    if (internalKey == null || internalKey.isBlank())
      throw new IllegalStateException("INTERNAL_SERVICE_KEY is required");
    this.verifier = verifier;
    this.internalKey = internalKey;
    this.defaultLimit = defaultLimit;
    this.aiLimit = aiLimit;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    String correlation =
        Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"))
            .filter(v -> !v.isBlank())
            .orElse(UUID.randomUUID().toString());
    ServerHttpRequest.Builder request =
        exchange
            .getRequest()
            .mutate()
            .header("X-Internal-Key", internalKey)
            .header("X-Correlation-Id", correlation);
    if (!PUBLIC.contains(path)) {
      String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
      if (auth == null || !auth.startsWith("Bearer "))
        return deny(
            exchange,
            HttpStatus.UNAUTHORIZED,
            "AUTH_REQUIRED",
            "A valid access token is required.");
      try {
        Claims c = verifier.verify(auth.substring(7));
        String userId = c.getSubject();
        if (!allow(userId, path))
          return deny(
              exchange,
              HttpStatus.TOO_MANY_REQUESTS,
              "RATE_LIMITED",
              "Too many requests. Please try again shortly.");
        request
            .headers(
                h -> {
                  h.remove("X-User-Id");
                  h.remove("X-User-Role");
                })
            .header("X-User-Id", userId)
            .header("X-User-Role", c.get("role", String.class));
      } catch (Exception ex) {
        return deny(
            exchange,
            HttpStatus.UNAUTHORIZED,
            "INVALID_TOKEN",
            "The access token is invalid or expired.");
      }
    }
    exchange.getResponse().getHeaders().set("X-Correlation-Id", correlation);
    return chain.filter(exchange.mutate().request(request.build()).build());
  }

  private boolean allow(String key, String path) {
    int max =
        path.startsWith("/api/chat") || path.startsWith("/api/analyses") ? aiLimit : defaultLimit;
    Window w =
        limits.compute(
            key,
            (k, v) ->
                v == null || v.started.plusSeconds(60).isBefore(Instant.now())
                    ? new Window(Instant.now(), new java.util.concurrent.atomic.AtomicInteger())
                    : v);
    return w.count.incrementAndGet() <= max;
  }

  private Mono<Void> deny(ServerWebExchange e, HttpStatus s, String code, String msg) {
    e.getResponse().setStatusCode(s);
    e.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    byte[] body =
        ("{\"success\":false,\"data\":null,\"error\":{\"code\":\""
                + code
                + "\",\"message\":\""
                + msg
                + "\"}}")
            .getBytes();
    return e.getResponse().writeWith(Mono.just(e.getResponse().bufferFactory().wrap(body)));
  }

  @Override
  public int getOrder() {
    return -100;
  }

  private record Window(Instant started, java.util.concurrent.atomic.AtomicInteger count) {}
}
