// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.exception;

/** Thrown when a uniqueness constraint would be violated. Rendered as 409. */
public class DuplicateResourceException extends RuntimeException {

  public DuplicateResourceException(String message) {
    super(message);
  }
}
