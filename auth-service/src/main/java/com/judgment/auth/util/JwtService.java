package com.judgment.auth.util;

import com.judgment.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Signs and validates short lived gateway access tokens. */
@Service
public class JwtService {
  private final SecretKey key;
  private final Duration ttl;

  public JwtService(
      @Value("${app.jwt-secret}") String secret, @Value("${app.access-minutes}") long minutes) {
    if (secret == null || secret.length() < 32)
      throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    ttl = Duration.ofMinutes(minutes);
  }

  public String issue(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("role", user.getRole().name())
        .claim("email", user.getEmail())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(ttl)))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public long expiresInSeconds() {
    return ttl.toSeconds();
  }
}
