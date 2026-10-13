package com.judgment.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.judgment.auth.dto.AuthDtos.*;
import com.judgment.auth.entity.*;
import com.judgment.auth.repository.*;
import com.judgment.auth.service.AuthService;
import com.judgment.auth.util.JwtService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock UserRepository users;
  @Mock RefreshTokenRepository tokens;

  @Test
  void registerHashesPassword() {
    when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
    when(users.save(any())).thenAnswer(i -> i.getArgument(0));
    AuthService s =
        new AuthService(
            users,
            tokens,
            new BCryptPasswordEncoder(),
            new JwtService("12345678901234567890123456789012", 15),
            7);
    assertNotNull(
        s.register(new RegisterRequest("user@example.com", "secure-password")).accessToken());
    verify(tokens).save(any());
  }
}
