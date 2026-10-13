package com.judgment.document.service;

import com.judgment.document.dto.DocumentDtos.*;
import com.judgment.document.entity.*;
import com.judgment.document.exception.ApiException;
import com.judgment.document.repository.DocumentRepository;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

/** Coordinates safe file storage, ownership checks and ingestion notifications. */
@Service
public class DocumentService {
  private final DocumentRepository docs;
  private final FileStorageService files;
  private final int pageLimit;
  private final RestClient client;
  private final String ingestionUrl, key;

  public DocumentService(
      DocumentRepository docs,
      FileStorageService files,
      @Value("${app.max-pdf-pages}") int pageLimit,
      @Value("${app.ingestion-url}") String ingestionUrl,
      @Value("${app.internal-key}") String key) {
    this.docs = docs;
    this.files = files;
    this.pageLimit = pageLimit;
    this.ingestionUrl = ingestionUrl;
    this.key = key;
    this.client = RestClient.create();
  }

  @Transactional
  public DocumentView upload(UUID userId, MultipartFile file, String correlation) {
    UUID id = UUID.randomUUID();
    FileStorageService.StoredFile stored = files.store(file, id);
    if (docs.existsByOwnerIdAndFileHash(userId, stored.hash())) {
      files.delete(stored.path());
      throw new ApiException(
          HttpStatus.CONFLICT, "DUPLICATE_DOCUMENT", "This PDF was already uploaded by this user.");
    }
    int pages = pages(stored.bytes());
    if (pages > pageLimit) {
      files.delete(stored.path());
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "PAGE_LIMIT", "The PDF exceeds the page limit.");
    }
    JudgmentDocument doc =
        docs.save(
            new JudgmentDocument(
                userId,
                title(stored.filename()),
                stored.filename(),
                stored.path(),
                stored.hash(),
                pages));
    doc.setStatus(DocumentStatus.PROCESSING);
    notifyIngestion(doc, correlation);
    return view(doc);
  }

  public List<DocumentView> list(UUID user) {
    return docs.findByOwnerIdOrderByCreatedAtDesc(user).stream().map(this::view).toList();
  }

  public DocumentView get(UUID user, UUID id) {
    return view(owned(user, id));
  }

  public Path download(UUID user, UUID id) {
    return files.path(owned(user, id).getStoragePath());
  }

  @Transactional
  public void delete(UUID user, UUID id, String correlation) {
    JudgmentDocument doc = owned(user, id);
    try {
      client
          .delete()
          .uri(ingestionUrl + "/v1/ingest/" + id)
          .header("X-Internal-Key", key)
          .header("X-Correlation-Id", correlation)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception ignored) {
    }
    files.delete(doc.getStoragePath());
    docs.delete(doc);
  }

  @Transactional
  public void status(UUID id, IngestionStatus update) {
    JudgmentDocument doc =
        docs.findById(id)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document was not found."));
    doc.setStatus(update.status());
    doc.metadata(update.court(), update.caseNumber(), update.judgmentDate());
  }

  public OwnershipView ownership(UUID user, List<UUID> ids) {
    Map<UUID, Boolean> result = new LinkedHashMap<>();
    for (UUID id : ids) result.put(id, docs.findByIdAndOwnerId(id, user).isPresent());
    return new OwnershipView(result);
  }

  private JudgmentDocument owned(UUID user, UUID id) {
    return docs.findByIdAndOwnerId(id, user)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document was not found."));
  }

  private int pages(byte[] bytes) {
    try (PDDocument pdf = Loader.loadPDF(bytes)) {
      return pdf.getNumberOfPages();
    } catch (IOException e) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PDF", "The PDF cannot be read.");
    }
  }

  private void notifyIngestion(JudgmentDocument d, String correlation) {
    try {
      client
          .post()
          .uri(ingestionUrl + "/v1/ingest")
          .header("X-Internal-Key", key)
          .header("X-Correlation-Id", correlation)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              Map.of(
                  "document_id",
                  d.getId().toString(),
                  "storage_path",
                  d.getStoragePath(),
                  "title",
                  d.getTitle()))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      d.setStatus(DocumentStatus.FAILED);
    }
  }

  private String title(String file) {
    return file.replaceFirst("(?i)\\.pdf$", "").replace('_', ' ').trim();
  }

  private DocumentView view(JudgmentDocument d) {
    return new DocumentView(
        d.getId(),
        d.getTitle(),
        d.getFilename(),
        d.getPageCount(),
        d.getStatus(),
        d.getCourt(),
        d.getCaseNumber(),
        d.getJudgmentDate(),
        d.getCreatedAt());
  }
}
