package com.judgment.gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Verifies the signed user claims before a request reaches private services. */
@Component
public class JwtVerifier {
  private final SecretKey key;

  public JwtVerifier(@Value("${app.jwt-secret}") String secret) {
    if (secret == null || secret.length() < 32)
      throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public Claims verify(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
