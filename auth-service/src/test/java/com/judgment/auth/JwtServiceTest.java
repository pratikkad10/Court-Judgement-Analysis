package com.judgment.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.judgment.auth.entity.*;
import com.judgment.auth.util.JwtService;
import org.junit.jupiter.api.*;

class JwtServiceTest {
  @Test
  void issuesAndParsesSubject() {
    JwtService jwt = new JwtService("12345678901234567890123456789012", 15);
    String token = jwt.issue(new User("a@example.com", "hash", Role.USER));
    assertNotNull(jwt.parse(token).getSubject());
    assertEquals("USER", jwt.parse(token).get("role", String.class));
  }
}
