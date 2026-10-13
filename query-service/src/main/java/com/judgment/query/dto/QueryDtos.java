package com.judgment.query.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class QueryDtos {
  private QueryDtos() {}

  public record CreateSession(@NotBlank @Size(max = 500) String title) {}

  public record AskRequest(
      @NotEmpty List<UUID> documentIds, @NotBlank @Size(max = 8000) String question) {}

  public record AnalysisRequest(@NotEmpty List<UUID> documentIds, @Size(max = 4000) String issue) {}

  public record SessionView(UUID id, String title, Instant createdAt, Instant updatedAt) {}

  public record MessageView(
      UUID id,
      String role,
      String content,
      Object citations,
      String providerUsed,
      Long latencyMs,
      Instant createdAt) {}

  public record AnalysisView(
      UUID id, String type, Object documentIds, Object result, Instant createdAt) {}

  public record Dashboard(long sessions, long messages, long analyses) {}

  public record AuditLogView(
      UUID id,
      UUID ownerId,
      String action,
      String correlationId,
      Object details,
      Instant createdAt) {}
}
