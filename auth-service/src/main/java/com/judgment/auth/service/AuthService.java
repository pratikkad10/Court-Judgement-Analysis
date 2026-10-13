package com.judgment.auth.service;

import com.judgment.auth.dto.AuthDtos.*;
import com.judgment.auth.entity.*;
import com.judgment.auth.exception.ApiException;
import com.judgment.auth.repository.*;
import com.judgment.auth.util.JwtService;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements account authentication and rotating refresh-token handling. */
@Service
public class AuthService {
  private final UserRepository users;
  private final RefreshTokenRepository tokens;
  private final PasswordEncoder encoder;
  private final JwtService jwt;
  private final Duration refreshTtl;
  private final SecureRandom random = new SecureRandom();

  public AuthService(
      UserRepository users,
      RefreshTokenRepository tokens,
      PasswordEncoder encoder,
      JwtService jwt,
      @Value("${app.refresh-days}") long days) {
    this.users = users;
    this.tokens = tokens;
    this.encoder = encoder;
    this.jwt = jwt;
    refreshTtl = Duration.ofDays(days);
  }

  @Transactional
  public TokenResponse register(RegisterRequest r) {
    String email = r.email().trim().toLowerCase(Locale.ROOT);
    if (users.findByEmailIgnoreCase(email).isPresent())
      throw new ApiException(
          HttpStatus.CONFLICT, "EMAIL_EXISTS", "An account already exists for this email.");
    return issue(users.save(new User(email, encoder.encode(r.password()), Role.USER)));
  }

  public TokenResponse login(LoginRequest r) {
    User u =
        users
            .findByEmailIgnoreCase(r.email().trim())
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS",
                        "Invalid email or password."));
    if (!u.isEnabled() || !encoder.matches(r.password(), u.getPasswordHash()))
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
    return issue(u);
  }

  @Transactional
  public TokenResponse refresh(String raw) {
    for (RefreshToken token : tokens.findAll())
      if (!token.isRevoked()
          && !token.getExpiresAt().isBefore(Instant.now())
          && encoder.matches(raw, token.getTokenHash())) {
        token.revoke();
        return issue(
            users
                .findById(token.getUserId())
                .orElseThrow(
                    () ->
                        new ApiException(
                            HttpStatus.UNAUTHORIZED,
                            "INVALID_REFRESH",
                            "Refresh token is invalid.")));
      }
    throw new ApiException(
        HttpStatus.UNAUTHORIZED, "INVALID_REFRESH", "Refresh token is invalid or expired.");
  }

  @Transactional
  public void logout(String raw) {
    for (RefreshToken token : tokens.findAll())
      if (encoder.matches(raw, token.getTokenHash())) token.revoke();
  }

  public UserView current(UUID id) {
    return view(
        users
            .findById(id)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found.")));
  }

  public List<UserView> list() {
    return users.findAll().stream().map(this::view).toList();
  }

  @Transactional
  public UserView enabled(UUID id, boolean enabled) {
    User u =
        users
            .findById(id)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found."));
    u.setEnabled(enabled);
    return view(u);
  }

  private TokenResponse issue(User u) {
    byte[] bytes = new byte[48];
    random.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    tokens.save(new RefreshToken(u.getId(), encoder.encode(raw), Instant.now().plus(refreshTtl)));
    return new TokenResponse(jwt.issue(u), raw, "Bearer", jwt.expiresInSeconds(), view(u));
  }

  private UserView view(User u) {
    return new UserView(u.getId(), u.getEmail(), u.getRole().name(), u.isEnabled());
  }
}
