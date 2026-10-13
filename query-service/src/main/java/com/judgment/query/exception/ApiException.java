package com.judgment.query.exception;

import org.springframework.http.HttpStatus;

/** Carries safe query orchestration errors. */
public class ApiException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  public ApiException(HttpStatus s, String c, String m) {
    super(m);
    status = s;
    code = c;
  }

  public HttpStatus status() {
    return status;
  }

  public String code() {
    return code;
  }
}
