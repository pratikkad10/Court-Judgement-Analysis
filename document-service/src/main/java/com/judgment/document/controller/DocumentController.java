package com.judgment.document.controller;

import com.judgment.document.dto.*;
import com.judgment.document.dto.DocumentDtos.*;
import com.judgment.document.service.DocumentService;
import jakarta.validation.Valid;
import java.nio.file.*;
import java.util.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Exposes authenticated PDF upload, retrieval and ingestion status callbacks. */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
  private final DocumentService service;

  public DocumentController(DocumentService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<ApiResponse<DocumentView>> upload(
      @RequestHeader("X-User-Id") UUID user,
      @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String correlation,
      @RequestPart("file") MultipartFile file) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(service.upload(user, file, correlation)));
  }

  @GetMapping
  ApiResponse<List<DocumentView>> list(@RequestHeader("X-User-Id") UUID user) {
    return ApiResponse.ok(service.list(user));
  }

  @GetMapping("/{id}")
  ApiResponse<DocumentView> get(@RequestHeader("X-User-Id") UUID user, @PathVariable UUID id) {
    return ApiResponse.ok(service.get(user, id));
  }

  @GetMapping("/{id}/download")
  ResponseEntity<FileSystemResource> download(
      @RequestHeader("X-User-Id") UUID user, @PathVariable UUID id) {
    Path file = service.download(user, id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=judgment.pdf")
        .body(new FileSystemResource(file));
  }

  @DeleteMapping("/{id}")
  ResponseEntity<ApiResponse<Void>> delete(
      @RequestHeader("X-User-Id") UUID user,
      @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String correlation,
      @PathVariable UUID id) {
    service.delete(user, id, correlation);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }

  @GetMapping("/internal/ownership")
  ApiResponse<OwnershipView> ownership(
      @RequestHeader("X-User-Id") UUID user, @RequestParam List<UUID> ids) {
    return ApiResponse.ok(service.ownership(user, ids));
  }

  @PostMapping("/internal/{id}/status")
  ApiResponse<Void> status(@PathVariable UUID id, @Valid @RequestBody IngestionStatus update) {
    service.status(id, update);
    return ApiResponse.ok(null);
  }
}
