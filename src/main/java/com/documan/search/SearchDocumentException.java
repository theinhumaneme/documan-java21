// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

/**
 * Meilisearch accepted the request but rejected the content — a malformed document, an unknown
 * field, or an indexing task that finished {@code FAILED}.
 *
 * <p>This is the only failure class that counts against a key's retry budget, because retrying it
 * unchanged will not help.
 */
public class SearchDocumentException extends RuntimeException {

  public SearchDocumentException(String message) {
    super(message);
  }

  public SearchDocumentException(String message, Throwable cause) {
    super(message, cause);
  }
}
