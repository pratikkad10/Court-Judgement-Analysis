package com.judgment.query.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Stores a reusable summary, argument extraction or comparison result. */
@Entity
@Table(name = "analyses")
public class Analysis {
  @Id private UUID id = UUID.randomUUID();

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(nullable = false)
  private String type;

  @Column(name = "document_ids", nullable = false, columnDefinition = "jsonb")
  private String documentIds;

  @Column(nullable = false, columnDefinition = "jsonb")
  private String result;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected Analysis() {}

  public Analysis(UUID ownerId, String type, String documentIds, String result) {
    this.ownerId = ownerId;
    this.type = type;
    this.documentIds = documentIds;
    this.result = result;
  }

  public UUID getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getDocumentIds() {
    return documentIds;
  }

  public String getResult() {
    return result;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
