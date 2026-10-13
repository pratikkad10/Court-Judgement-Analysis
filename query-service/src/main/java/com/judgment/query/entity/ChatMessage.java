package com.judgment.query.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Persists both user and grounded assistant turns with citation JSON. */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
  @Id private UUID id = UUID.randomUUID();

  @Column(name = "session_id", nullable = false)
  private UUID sessionId;

  @Column(nullable = false)
  private String role;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(nullable = false, columnDefinition = "jsonb")
  private String citations = "[]";

  @Column(name = "provider_used")
  private String providerUsed;

  @Column(name = "latency_ms")
  private Long latencyMs;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected ChatMessage() {}

  public ChatMessage(
      UUID sessionId,
      String role,
      String content,
      String citations,
      String provider,
      Long latency) {
    this.sessionId = sessionId;
    this.role = role;
    this.content = content;
    this.citations = citations;
    this.providerUsed = provider;
    this.latencyMs = latency;
  }

  public UUID getId() {
    return id;
  }

  public String getRole() {
    return role;
  }

  public String getContent() {
    return content;
  }

  public String getCitations() {
    return citations;
  }

  public String getProviderUsed() {
    return providerUsed;
  }

  public Long getLatencyMs() {
    return latencyMs;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
