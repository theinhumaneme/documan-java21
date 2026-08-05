// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

/**
 * Meilisearch was unreachable, timed out, or returned a server error.
 *
 * <p>Distinguished from a document-level rejection because it must <em>not</em> consume a key's
 * retry budget: an outage would otherwise drive the entire backlog past the attempt limit and
 * dead-letter it moments before the service came back.
 */
public class SearchTransportException extends RuntimeException {

  public SearchTransportException(String message, Throwable cause) {
    super(message, cause);
  }
}
