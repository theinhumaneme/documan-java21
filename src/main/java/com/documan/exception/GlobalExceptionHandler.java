// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.exception;

import com.documan.search.SearchUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Single place where exceptions become responses. Replaces the per-method try/catch blocks that
 * were duplicated across all nine controllers and returned bare strings.
 *
 * <p>Responses follow RFC 9457 ({@code application/problem+json}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final URI TYPE_NOT_FOUND = URI.create("https://documan.dev/problems/not-found");
  private static final URI TYPE_CONFLICT = URI.create("https://documan.dev/problems/conflict");
  private static final URI TYPE_VALIDATION = URI.create("https://documan.dev/problems/validation");
  private static final URI TYPE_STORAGE = URI.create("https://documan.dev/problems/storage");
  private static final URI TYPE_SEARCH =
      URI.create("https://documan.dev/problems/search-unavailable");
  private static final URI TYPE_INTERNAL = URI.create("https://documan.dev/problems/internal");
  private static final URI TYPE_AUTH = URI.create("https://documan.dev/problems/unauthenticated");
  private static final URI TYPE_FORBIDDEN = URI.create("https://documan.dev/problems/forbidden");

  /**
   * Security failures raised inside the dispatch rather than by the filter chain — a
   * {@code @PreAuthorize} on a controller or service method, or {@link
   * com.documan.security.CurrentUser#require()} on a request that turned out to be anonymous. The
   * chain's own rejections never reach here; those are translated before the DispatcherServlet.
   *
   * <p>These two exist because of the {@code Exception} catch-all below. Without them Spring
   * Security's exceptions match it and every authorisation failure is reported as a 500 — a client
   * cannot tell "sign in again" from "the server is broken", and the log fills with stack traces
   * for requests that were correctly refused.
   */
  @ExceptionHandler(AuthenticationException.class)
  ProblemDetail handleUnauthenticated(AuthenticationException ex, HttpServletRequest request) {
    return problem(HttpStatus.UNAUTHORIZED, "Not signed in", ex.getMessage(), TYPE_AUTH, request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleForbidden(AccessDeniedException ex, HttpServletRequest request) {
    return problem(HttpStatus.FORBIDDEN, "Not permitted", ex.getMessage(), TYPE_FORBIDDEN, request);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), TYPE_NOT_FOUND, request);
  }

  @ExceptionHandler(DuplicateResourceException.class)
  ProblemDetail handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
    return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), TYPE_CONFLICT, request);
  }

  /** Rules spanning more than one field, which bean validation cannot express on its own. */
  @ExceptionHandler(InvalidRequestException.class)
  ProblemDetail handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage(), TYPE_VALIDATION, request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn("Constraint violation on {}", request.getRequestURI(), ex);
    return problem(
        HttpStatus.CONFLICT,
        "Conflict",
        "The request violates a database constraint",
        TYPE_CONFLICT,
        request);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLock(
      OptimisticLockingFailureException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT,
        "Concurrent modification",
        "The resource was modified by another request; retry with a fresh copy",
        TYPE_CONFLICT,
        request);
  }

  @ExceptionHandler(StorageException.class)
  ProblemDetail handleStorage(StorageException ex, HttpServletRequest request) {
    log.error("Object storage failure on {}", request.getRequestURI(), ex);
    return problem(
        HttpStatus.BAD_GATEWAY, "Storage failure", ex.getMessage(), TYPE_STORAGE, request);
  }

  /**
   * Search being down is an availability problem, not a client error, and it must not look like
   * one: a 5xx with Retry-After tells callers and probes to come back rather than to give up.
   */
  @ExceptionHandler(SearchUnavailableException.class)
  ResponseEntity<ProblemDetail> handleSearchUnavailable(
      SearchUnavailableException ex, HttpServletRequest request) {
    ProblemDetail detail =
        problem(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Search unavailable",
            ex.getMessage(),
            TYPE_SEARCH,
            request);
    detail.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getRetryAfterSeconds()))
        .body(detail);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error",
        "An unexpected error occurred while processing the request",
        TYPE_INTERNAL,
        request);
  }

  /** Field-level validation failures are surfaced as a stable {@code errors} map. */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    Map<String, String> errors = new TreeMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      errors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }

    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "One or more fields failed validation");
    detail.setType(TYPE_VALIDATION);
    detail.setTitle("Validation failed");
    detail.setProperty("timestamp", OffsetDateTime.now());
    detail.setProperty("errors", new LinkedHashMap<>(errors));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detail);
  }

  private ProblemDetail problem(
      HttpStatus status, String title, String detailText, URI type, HttpServletRequest request) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, detailText);
    detail.setType(type);
    detail.setTitle(title);
    detail.setInstance(URI.create(request.getRequestURI()));
    detail.setProperty("timestamp", OffsetDateTime.now());
    return detail;
  }
}
