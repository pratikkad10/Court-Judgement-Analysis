package com.judgment.document.dto;

/** Provides the standard Java-service response envelope. */
public record ApiResponse<T>(boolean success, T data, ApiError error) {
  public static <T> ApiResponse<T> ok(T value) {
    return new ApiResponse<>(true, value, null);
  }

  public static <T> ApiResponse<T> fail(String code, String message) {
    return new ApiResponse<>(false, null, new ApiError(code, message));
  }

  public record ApiError(String code, String message) {}
}
