// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.exception;

/** Thrown when an addressed entity does not exist. Rendered as 404 by the global handler. */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String resource, Object id) {
    super("%s %s was not found".formatted(resource, id));
  }

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
