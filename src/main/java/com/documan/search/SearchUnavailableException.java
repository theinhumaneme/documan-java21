// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

/**
 * Raised by the query side when search cannot be served. Rendered as 503 with {@code Retry-After}.
 */
public class SearchUnavailableException extends RuntimeException {

  private final long retryAfterSeconds;

  public SearchUnavailableException(String message, long retryAfterSeconds, Throwable cause) {
    super(message, cause);
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
