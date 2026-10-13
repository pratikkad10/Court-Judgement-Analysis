package com.judgment.document.exception;

import com.judgment.document.dto.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Converts validation and domain errors into a single stable API shape. */
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiResponse<Void>> api(ApiException e) {
    return ResponseEntity.status(e.status()).body(ApiResponse.fail(e.code(), e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
    return ResponseEntity.status(500)
        .body(ApiResponse.fail("INTERNAL_ERROR", "An unexpected error occurred."));
  }
}
