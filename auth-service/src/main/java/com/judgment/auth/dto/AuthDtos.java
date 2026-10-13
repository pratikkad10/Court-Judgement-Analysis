package com.judgment.auth.dto;

import jakarta.validation.constraints.*;
import java.util.*;

public final class AuthDtos {
  private AuthDtos() {}

  public record RegisterRequest(
      @Email @NotBlank String email, @Size(min = 12, max = 100) String password) {}

  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record TokenResponse(
      String accessToken,
      String refreshToken,
      String tokenType,
      long expiresInSeconds,
      UserView user) {}

  public record UserView(UUID id, String email, String role, boolean enabled) {}
}
