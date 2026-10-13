package com.judgment.document.service;

import com.judgment.document.exception.ApiException;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Validates and writes PDF bytes using UUID paths rather than client filenames. */
@Service
public class FileStorageService {
  private final Path root;
  private final long maxBytes;

  public FileStorageService(
      @Value("${app.storage-dir}") String storage, @Value("${app.max-upload-mb}") long mb)
      throws IOException {
    root = Paths.get(storage).toAbsolutePath().normalize();
    Files.createDirectories(root);
    maxBytes = mb * 1024 * 1024;
  }

  public StoredFile store(MultipartFile file, UUID id) {
    if (file.isEmpty() || file.getSize() > maxBytes)
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "INVALID_SIZE",
          "The uploaded file is empty or exceeds the size limit.");
    String original = Optional.ofNullable(file.getOriginalFilename()).orElse("judgment.pdf");
    String name = Paths.get(original).getFileName().toString().replaceAll("[^A-Za-z0-9._ -]", "_");
    if (!name.toLowerCase(Locale.ROOT).endsWith(".pdf"))
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "INVALID_FILE", "Only PDF files are accepted.");
    try (InputStream in = file.getInputStream()) {
      byte[] bytes = in.readAllBytes();
      if (bytes.length < 5
          || bytes[0] != '%'
          || bytes[1] != 'P'
          || bytes[2] != 'D'
          || bytes[3] != 'F'
          || bytes[4] != '-')
        throw new ApiException(
            HttpStatus.BAD_REQUEST, "INVALID_PDF", "The file does not have valid PDF magic bytes.");
      String hash = hex(MessageDigest.getInstance("SHA-256").digest(bytes));
      Path target = root.resolve(id + ".pdf");
      Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
      return new StoredFile(name, target.toString(), hash, bytes);
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(
          HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", "The PDF could not be stored.");
    }
  }

  public Path path(String stored) {
    Path p = Paths.get(stored).normalize();
    if (!p.startsWith(root))
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PATH", "Invalid storage path.");
    return p;
  }

  public void delete(String stored) {
    try {
      Files.deleteIfExists(path(stored));
    } catch (IOException e) {
      throw new ApiException(
          HttpStatus.INTERNAL_SERVER_ERROR, "DELETE_ERROR", "The stored PDF could not be deleted.");
    }
  }

  private String hex(byte[] data) {
    StringBuilder s = new StringBuilder();
    for (byte b : data) s.append(String.format("%02x", b));
    return s.toString();
  }

  public record StoredFile(String filename, String path, String hash, byte[] bytes) {}
}
