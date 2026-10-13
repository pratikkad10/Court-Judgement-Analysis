package com.judgment.document.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Holds document metadata while bytes remain outside the database. */
@Entity
@Table(name = "documents")
public class JudgmentDocument {
  @Id private UUID id = UUID.randomUUID();

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String filename;

  @Column(name = "storage_path", nullable = false)
  private String storagePath;

  @Column(name = "file_hash", nullable = false)
  private String fileHash;

  @Column(name = "page_count", nullable = false)
  private int pageCount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DocumentStatus status = DocumentStatus.UPLOADED;

  private String court;

  @Column(name = "case_number")
  private String caseNumber;

  @Column(name = "judgment_date")
  private LocalDate judgmentDate;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected JudgmentDocument() {}

  public JudgmentDocument(
      UUID ownerId,
      String title,
      String filename,
      String storagePath,
      String fileHash,
      int pageCount) {
    this.ownerId = ownerId;
    this.title = title;
    this.filename = filename;
    this.storagePath = storagePath;
    this.fileHash = fileHash;
    this.pageCount = pageCount;
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

  public String getFilename() {
    return filename;
  }

  public String getStoragePath() {
    return storagePath;
  }

  public int getPageCount() {
    return pageCount;
  }

  public DocumentStatus getStatus() {
    return status;
  }

  public String getCourt() {
    return court;
  }

  public String getCaseNumber() {
    return caseNumber;
  }

  public LocalDate getJudgmentDate() {
    return judgmentDate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setStatus(DocumentStatus s) {
    status = s;
  }

  public void metadata(String court, String caseNumber, LocalDate date) {
    this.court = court;
    this.caseNumber = caseNumber;
    this.judgmentDate = date;
  }
}
