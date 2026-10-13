package com.judgment.document.dto;

import com.judgment.document.entity.DocumentStatus;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class DocumentDtos {
  private DocumentDtos() {}

  public record DocumentView(
      UUID id,
      String title,
      String filename,
      int pageCount,
      DocumentStatus status,
      String court,
      String caseNumber,
      LocalDate judgmentDate,
      Instant createdAt) {}

  public record IngestionStatus(
      @NotNull DocumentStatus status,
      String court,
      String caseNumber,
      LocalDate judgmentDate,
      String failureReason) {}

  public record OwnershipView(Map<UUID, Boolean> owned) {}
}
