package com.judgment.query.exception;

import com.judgment.query.dto.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Keeps error bodies safe and consistent. */
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiResponse<Void>> api(ApiException e) {
    return ResponseEntity.status(e.status()).body(ApiResponse.fail(e.code(), e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> other(Exception e) {
    return ResponseEntity.status(500)
        .body(ApiResponse.fail("INTERNAL_ERROR", "An unexpected error occurred."));
  }
}
