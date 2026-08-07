// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.exception;

/**
 * Thrown when a request is well-formed but asks for something incoherent. Rendered as 400.
 *
 * <p>Distinct from {@link ResourceNotFoundException}, which says a named thing does not exist, and
 * from {@link DuplicateResourceException}, which says the current state forbids the change. This
 * one says the request itself does not make sense — a move with two destinations, or with none —
 * and the distinction is not cosmetic: 404 invites a client to go and look for the missing thing,
 * and a retry will fail the same way until the request is corrected.
 *
 * <p>Field-level problems are already covered by bean validation, which the global handler turns
 * into a 400 with an {@code errors} map. This is for rules that span more than one field, which an
 * annotation cannot express without a custom constraint.
 */
public class InvalidRequestException extends RuntimeException {

  public InvalidRequestException(String message) {
    super(message);
  }
}
