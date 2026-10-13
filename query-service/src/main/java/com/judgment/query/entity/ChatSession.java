package com.judgment.query.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Groups a user's research conversation and preserves ownership. */
@Entity
@Table(name = "chat_sessions")
public class ChatSession {
  @Id private UUID id = UUID.randomUUID();

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(nullable = false)
  private String title;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected ChatSession() {}

  public ChatSession(UUID ownerId, String title) {
    this.ownerId = ownerId;
    this.title = title;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public String getTitle() {
    return title;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void touch() {
    updatedAt = Instant.now();
  }
}
