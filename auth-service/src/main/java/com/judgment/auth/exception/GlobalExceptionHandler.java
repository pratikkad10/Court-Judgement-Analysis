package com.judgment.auth.exception;

import com.judgment.auth.dto.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Converts expected failures into the shared response envelope. */
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiResponse<Void>> api(ApiException e) {
    return ResponseEntity.status(e.status()).body(ApiResponse.fail(e.code(), e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.fail("INTERNAL_ERROR", "An unexpected error occurred."));
  }
}
