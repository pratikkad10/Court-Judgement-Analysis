package com.judgment.query.controller;

import com.judgment.query.dto.*;
import com.judgment.query.dto.QueryDtos.*;
import com.judgment.query.exception.ApiException;
import com.judgment.query.service.ResearchService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Serves chat, saved analyses, history and role-protected dashboard data. */
@RestController
public class QueryController {
  private final ResearchService service;

  public QueryController(ResearchService s) {
    service = s;
  }

  @PostMapping("/api/chat/sessions")
  ResponseEntity<ApiResponse<SessionView>> create(
      @RequestHeader("X-User-Id") UUID u, @Valid @RequestBody CreateSession r) {
    return ResponseEntity.status(201).body(ApiResponse.ok(service.create(u, r)));
  }

  @GetMapping("/api/chat/sessions")
  ApiResponse<List<SessionView>> sessions(@RequestHeader("X-User-Id") UUID u) {
    return ApiResponse.ok(service.sessions(u));
  }

  @GetMapping("/api/chat/sessions/{id}/messages")
  ApiResponse<List<MessageView>> messages(
      @RequestHeader("X-User-Id") UUID u, @PathVariable UUID id) {
    return ApiResponse.ok(service.messages(u, id));
  }

  @PostMapping("/api/chat/sessions/{id}/messages")
  ApiResponse<MessageView> ask(
      @RequestHeader("X-User-Id") UUID u,
      @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String c,
      @PathVariable UUID id,
      @Valid @RequestBody AskRequest r) {
    return ApiResponse.ok(service.ask(u, id, r, c));
  }

  @PostMapping("/api/analyses/{type}")
  ApiResponse<AnalysisView> analysis(
      @RequestHeader("X-User-Id") UUID u,
      @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String c,
      @PathVariable String type,
      @Valid @RequestBody AnalysisRequest r) {
    return ApiResponse.ok(service.analyse(u, type, r, c));
  }

  @GetMapping("/api/history")
  ApiResponse<List<AnalysisView>> history(@RequestHeader("X-User-Id") UUID u) {
    return ApiResponse.ok(service.history(u));
  }

  @GetMapping("/api/dashboard")
  ApiResponse<Dashboard> dashboard(@RequestHeader("X-User-Id") UUID u) {
    return ApiResponse.ok(service.dashboard(u));
  }

  @GetMapping("/api/dashboard/admin")
  ApiResponse<Map<String, Object>> admin(@RequestHeader("X-User-Role") String role) {
    if (!"ADMIN".equals(role))
      throw new ApiException(
          HttpStatus.FORBIDDEN, "FORBIDDEN", "Administrator access is required.");
    return ApiResponse.ok(service.adminDashboard());
  }

  @GetMapping("/api/dashboard/admin/audit")
  ApiResponse<List<AuditLogView>> audit(@RequestHeader("X-User-Role") String role) {
    if (!"ADMIN".equals(role))
      throw new ApiException(
          HttpStatus.FORBIDDEN, "FORBIDDEN", "Administrator access is required.");
    return ApiResponse.ok(service.auditLogs());
  }
}
