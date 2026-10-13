package com.judgment.query.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.judgment.query.dto.QueryDtos.*;
import com.judgment.query.entity.*;
import com.judgment.query.exception.ApiException;
import com.judgment.query.repository.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/** Enforces ownership before forwarding narrowly scoped work to the RAG service. */
@Service
public class ResearchService {
  private final ChatSessionRepository sessions;
  private final ChatMessageRepository messages;
  private final AnalysisRepository analyses;
  private final AuditLogRepository audit;
  private final ObjectMapper json;
  private final RestClient client = RestClient.create();
  private final String documentsUrl, ragUrl, key;

  public ResearchService(
      ChatSessionRepository sessions,
      ChatMessageRepository messages,
      AnalysisRepository analyses,
      AuditLogRepository audit,
      ObjectMapper json,
      @Value("${app.document-service-url}") String documentsUrl,
      @Value("${app.rag-service-url}") String ragUrl,
      @Value("${app.internal-key}") String key) {
    this.sessions = sessions;
    this.messages = messages;
    this.analyses = analyses;
    this.audit = audit;
    this.json = json;
    this.documentsUrl = documentsUrl;
    this.ragUrl = ragUrl;
    this.key = key;
  }

  @Transactional
  public SessionView create(UUID user, CreateSession request) {
    ChatSession session = sessions.save(new ChatSession(user, request.title().trim()));
    return session(session);
  }

  @Transactional
  public MessageView ask(UUID user, UUID sessionId, AskRequest request, String correlation) {
    ChatSession session = ownedSession(user, sessionId);
    requireOwned(user, request.documentIds(), correlation);
    messages.save(new ChatMessage(sessionId, "USER", request.question(), "[]", null, null));
    List<Map<String, Object>> history =
        messages.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
            .skip(Math.max(0, messages.findBySessionIdOrderByCreatedAtAsc(sessionId).size() - 6))
            .map(
                m ->
                    Map.<String, Object>of(
                        "role", m.getRole().toLowerCase(Locale.ROOT), "content", m.getContent()))
            .toList();
    Map<String, Object> output =
        callRag(
            "/v1/query",
            Map.of(
                "document_ids",
                request.documentIds(),
                "question",
                request.question(),
                "history",
                history),
            correlation);
    ChatMessage reply =
        messages.save(
            new ChatMessage(
                sessionId,
                "ASSISTANT",
                String.valueOf(output.getOrDefault("answer", "")),
                write(output.getOrDefault("citations", List.of())),
                asString(output.get("provider_used")),
                asLong(output.get("latency_ms"))));
    session.touch();
    audit.save(
        new AuditLog(user, "CHAT_QUERY", correlation, "{\"sessionId\":\"" + sessionId + "\"}"));
    return message(reply);
  }

  @Transactional
  public AnalysisView analyse(UUID user, String type, AnalysisRequest request, String correlation) {
    if (!Set.of("summary", "arguments", "compare").contains(type))
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "INVALID_ANALYSIS", "Unsupported analysis type.");
    requireOwned(user, request.documentIds(), correlation);
    Map<String, Object> result =
        callRag(
            "/v1/" + type,
            Map.of(
                "document_ids",
                request.documentIds(),
                "issue",
                Optional.ofNullable(request.issue()).orElse("")),
            correlation);
    Analysis analysis =
        analyses.save(new Analysis(user, type, write(request.documentIds()), write(result)));
    audit.save(new AuditLog(user, "ANALYSIS_" + type.toUpperCase(Locale.ROOT), correlation, "{}"));
    return analysis(analysis);
  }

  @Transactional(readOnly = true)
  public List<SessionView> sessions(UUID user) {
    return sessions.findByOwnerIdOrderByUpdatedAtDesc(user).stream().map(this::session).toList();
  }

  @Transactional(readOnly = true)
  public List<MessageView> messages(UUID user, UUID sessionId) {
    ownedSession(user, sessionId);
    return messages.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
        .map(this::message)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AnalysisView> history(UUID user) {
    return analyses.findByOwnerIdOrderByCreatedAtDesc(user).stream().map(this::analysis).toList();
  }

  @Transactional(readOnly = true)
  public Dashboard dashboard(UUID user) {
    long count = sessions.findByOwnerIdOrderByUpdatedAtDesc(user).size();
    long analysesCount = analyses.findByOwnerIdOrderByCreatedAtDesc(user).size();
    long messageCount =
        sessions.findByOwnerIdOrderByUpdatedAtDesc(user).stream()
            .mapToLong(s -> messages.findBySessionIdOrderByCreatedAtAsc(s.getId()).size())
            .sum();
    return new Dashboard(count, messageCount, analysesCount);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> adminDashboard() {
    return Map.of(
        "sessions",
        sessions.count(),
        "messages",
        messages.count(),
        "analyses",
        analyses.count(),
        "auditLogs",
        audit.count());
  }

  @Transactional(readOnly = true)
  public List<AuditLogView> auditLogs() {
    return audit.findAll().stream()
        .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
        .limit(100)
        .map(
            log ->
                new AuditLogView(
                    log.getId(),
                    log.getOwnerId(),
                    log.getAction(),
                    log.getCorrelationId(),
                    read(log.getDetails()),
                    log.getCreatedAt()))
        .toList();
  }

  private void requireOwned(UUID user, List<UUID> ids, String correlation) {
    if (ids.isEmpty())
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "NO_DOCUMENTS", "Select at least one ready judgment.");
    String query = ids.stream().map(UUID::toString).reduce((a, b) -> a + "," + b).orElse("");
    try {
      Map<String, Object> envelope =
          client
              .get()
              .uri(documentsUrl + "/api/documents/internal/ownership?ids=" + query)
              .header("X-Internal-Key", key)
              .header("X-User-Id", user.toString())
              .header("X-Correlation-Id", correlation)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      Map<String, Object> data = (Map<String, Object>) envelope.get("data");
      Map<String, Object> owned = (Map<String, Object>) data.get("owned");
      boolean all = ids.stream().allMatch(id -> Boolean.TRUE.equals(owned.get(id.toString())));
      if (!all)
        throw new ApiException(
            HttpStatus.FORBIDDEN, "DOCUMENT_FORBIDDEN", "You do not own every selected judgment.");
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "DOCUMENT_SERVICE_UNAVAILABLE",
          "Document ownership could not be verified.");
    }
  }

  private Map<String, Object> callRag(String path, Map<String, Object> body, String correlation) {
    try {
      return client
          .post()
          .uri(ragUrl + path)
          .header("X-Internal-Key", key)
          .header("X-Correlation-Id", correlation)
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});
    } catch (Exception e) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "AI_SERVICE_UNAVAILABLE",
          "The research service is temporarily unavailable.");
    }
  }

  private ChatSession ownedSession(UUID user, UUID id) {
    return sessions
        .findByIdAndOwnerId(id, user)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Chat session was not found."));
  }

  private SessionView session(ChatSession s) {
    return new SessionView(s.getId(), s.getTitle(), s.getCreatedAt(), s.getUpdatedAt());
  }

  private MessageView message(ChatMessage m) {
    return new MessageView(
        m.getId(),
        m.getRole(),
        m.getContent(),
        read(m.getCitations()),
        m.getProviderUsed(),
        m.getLatencyMs(),
        m.getCreatedAt());
  }

  private AnalysisView analysis(Analysis a) {
    return new AnalysisView(
        a.getId(), a.getType(), read(a.getDocumentIds()), read(a.getResult()), a.getCreatedAt());
  }

  private String write(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("JSON serialization failed", e);
    }
  }

  private Object read(String value) {
    try {
      return json.readValue(value, Object.class);
    } catch (JsonProcessingException e) {
      return List.of();
    }
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private Long asLong(Object value) {
    return value instanceof Number n ? n.longValue() : null;
  }
}
