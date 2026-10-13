package com.judgment.auth.controller;

import com.judgment.auth.dto.*;
import com.judgment.auth.dto.AuthDtos.*;
import com.judgment.auth.exception.ApiException;
import com.judgment.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Exposes authentication and administrator account operations. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/register")
  ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.register(r)));
  }

  @PostMapping("/login")
  ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest r) {
    return ApiResponse.ok(service.login(r));
  }

  @PostMapping("/refresh")
  ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest r) {
    return ApiResponse.ok(service.refresh(r.refreshToken()));
  }

  @PostMapping("/logout")
  ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest r) {
    service.logout(r.refreshToken());
    return ResponseEntity.ok(ApiResponse.ok(null));
  }

  @GetMapping("/me")
  ApiResponse<UserView> me(@RequestHeader("X-User-Id") UUID userId) {
    return ApiResponse.ok(service.current(userId));
  }

  @GetMapping("/admin/users")
  ApiResponse<List<UserView>> users(@RequestHeader("X-User-Role") String role) {
    admin(role);
    return ApiResponse.ok(service.list());
  }

  @PatchMapping("/admin/users/{id}/enabled")
  ApiResponse<UserView> enabled(
      @PathVariable UUID id,
      @RequestParam boolean value,
      @RequestHeader("X-User-Role") String role) {
    admin(role);
    return ApiResponse.ok(service.enabled(id, value));
  }

  private void admin(String role) {
    if (!"ADMIN".equals(role))
      throw new ApiException(
          HttpStatus.FORBIDDEN, "FORBIDDEN", "Administrator access is required.");
  }
}
