package com.judgment.gateway;

import static org.junit.jupiter.api.Assertions.*;

import com.judgment.gateway.util.JwtVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;

class JwtVerifierTest {
  @Test
  void rejectsTamperedToken() {
    String secret = "12345678901234567890123456789012";
    String token =
        Jwts.builder()
            .subject("1")
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .compact();
    assertEquals("1", new JwtVerifier(secret).verify(token).getSubject());
    assertThrows(
        Exception.class, () -> new JwtVerifier("abcdefghijabcdefghijabcdefghijab").verify(token));
  }
}
