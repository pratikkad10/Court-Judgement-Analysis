package com.judgment.query.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Captures non-sensitive action metadata for admin auditing. */
@Entity
@Table(name = "audit_logs")
public class AuditLog {
  @Id private UUID id = UUID.randomUUID();

  @Column(name = "owner_id")
  private UUID ownerId;

  @Column(nullable = false)
  private String action;

  @Column(name = "correlation_id")
  private String correlationId;

  @Column(nullable = false, columnDefinition = "jsonb")
  private String details = "{}";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected AuditLog() {}

  public AuditLog(UUID owner, String action, String correlation, String details) {
    ownerId = owner;
    this.action = action;
    correlationId = correlation;
    this.details = details;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public String getAction() {
    return action;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getDetails() {
    return details;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
