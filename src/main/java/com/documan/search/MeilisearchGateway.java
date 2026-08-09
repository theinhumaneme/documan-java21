// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchCommunicationException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.exceptions.MeilisearchTimeoutException;
import com.meilisearch.sdk.model.Task;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TaskStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Every call into Meilisearch goes through here, so three concerns live in one place: bounding how
 * long a call may take, classifying failures, and confirming that work actually completed.
 *
 * <p>That last one matters more than it looks. {@code addDocuments} returns HTTP 202 and a task
 * handle — the documents are queued, not indexed. Treating the 202 as success and clearing the
 * outbox row would silently lose every change whose task later failed, so each write is followed to
 * a terminal task status before it is reported as done.
 */
@Component
@ConditionalOnSearchEnabled
public class MeilisearchGateway {

  private static final Logger log = LoggerFactory.getLogger(MeilisearchGateway.class);

  /** Consecutive transport failures before calls fail fast instead of waiting on a socket. */
  private static final int FAILURES_BEFORE_OPEN = 3;

  private static final Duration OPEN_DURATION = Duration.ofSeconds(30);

  private final Client client;
  private final SearchProperties properties;
  private final ExecutorService timeoutExecutor =
      Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("meili-call-", 0).factory());

  private final AtomicInteger consecutiveTransportFailures = new AtomicInteger();
  private final AtomicReference<Instant> openUntil = new AtomicReference<>(Instant.EPOCH);

  public MeilisearchGateway(Client client, SearchProperties properties) {
    this.client = client;
    this.properties = properties;
  }

  public Index index(SearchIndex index) {
    return call("index " + index, () -> client.index(index.uid(properties.indexPrefix())));
  }

  /** Full document replace. Waits for the indexing task to reach a terminal state. */
  public void upsert(SearchIndex index, String documentsJson) {
    Index target = index(index);
    TaskInfo info = call("addDocuments " + index, () -> target.addDocuments(documentsJson, "id"));
    awaitTask(target, info, "addDocuments " + index);
  }

  /**
   * Partial merge, used for the counter-only path so a vote does not force Meilisearch to
   * re-tokenise a whole post body to change an integer.
   */
  public void partialUpdate(SearchIndex index, String documentsJson) {
    Index target = index(index);
    TaskInfo info =
        call("updateDocuments " + index, () -> target.updateDocuments(documentsJson, "id"));
    awaitTask(target, info, "updateDocuments " + index);
  }

  public void delete(SearchIndex index, List<String> documentIds) {
    if (documentIds.isEmpty()) {
      return;
    }
    Index target = index(index);
    TaskInfo info = call("deleteDocuments " + index, () -> target.deleteDocuments(documentIds));
    awaitTask(target, info, "deleteDocuments " + index);
  }

  /** Empties an index. Used by tests to isolate cases; not called by application code. */
  public void deleteAll(SearchIndex index) {
    Index target = index(index);
    TaskInfo info = call("deleteAllDocuments " + index, target::deleteAllDocuments);
    awaitTask(target, info, "deleteAllDocuments " + index);
  }

  public String rawSearch(SearchIndex index, SearchRequest request) {
    Index target = index(index);
    return call("search " + index, () -> target.rawSearch(request));
  }

  public boolean healthy() {
    try {
      return Boolean.TRUE.equals(call("health", client::isHealthy));
    } catch (RuntimeException e) {
      return false;
    }
  }

  /** True while the breaker is open, so callers can skip work that is certain to fail. */
  public boolean degraded() {
    return Instant.now().isBefore(openUntil.get());
  }

  /**
   * A terminal task status is the only proof the write landed. {@code waitForTask} returns once the
   * task leaves the queue, including when it left by failing, so the status is checked explicitly.
   */
  private void awaitTask(Index target, TaskInfo info, String description) {
    int taskUid = info.getTaskUid();
    long timeoutMs = properties.requestTimeout().toMillis();
    call(
        description + " wait",
        () -> {
          target.waitForTask(taskUid, (int) timeoutMs, 50);
          return null;
        });

    Task task = call(description + " status", () -> client.getTask(taskUid));
    TaskStatus status = task.getStatus();
    if (status == TaskStatus.SUCCEEDED) {
      return;
    }
    String detail =
        task.getError() == null
            ? "task " + taskUid + " finished " + status
            : "task %d finished %s: %s".formatted(taskUid, status, task.getError().getMessage());
    // A rejected or cancelled task will be rejected again unchanged, so this counts as a
    // document-level failure and consumes the key's retry budget.
    throw new SearchDocumentException(detail);
  }

  /**
   * Applies the request timeout and maps SDK exceptions onto the transport/document split that the
   * outbox retry policy depends on.
   */
  private <T> T call(String description, Callable<T> operation) {
    if (degraded()) {
      throw new SearchTransportException("Meilisearch circuit open, skipping " + description, null);
    }

    Future<T> future = timeoutExecutor.submit(operation);
    try {
      T result = future.get(properties.requestTimeout().toMillis(), TimeUnit.MILLISECONDS);
      consecutiveTransportFailures.set(0);
      return result;
    } catch (TimeoutException e) {
      future.cancel(true);
      throw transportFailure(description + " timed out", e);
    } catch (InterruptedException e) {
      future.cancel(true);
      Thread.currentThread().interrupt();
      throw transportFailure(description + " interrupted", e);
    } catch (ExecutionException e) {
      throw classify(description, e.getCause());
    }
  }

  private RuntimeException classify(String description, Throwable cause) {
    return switch (cause) {
      case MeilisearchCommunicationException e ->
          transportFailure(description + " could not reach Meilisearch", e);
      case MeilisearchTimeoutException e -> transportFailure(description + " timed out", e);
      case MeilisearchApiException e -> {
        consecutiveTransportFailures.set(0);
        yield new SearchDocumentException(
            "%s rejected: %s (%s)".formatted(description, e.getMessage(), e.getCode()), e);
      }
      case MeilisearchException e ->
          new SearchDocumentException(description + " failed: " + e.getMessage(), e);
      case SearchDocumentException e -> e;
      case RuntimeException e -> transportFailure(description + " failed", e);
      default -> transportFailure(description + " failed", cause);
    };
  }

  private SearchTransportException transportFailure(String message, Throwable cause) {
    if (consecutiveTransportFailures.incrementAndGet() >= FAILURES_BEFORE_OPEN) {
      openUntil.set(Instant.now().plus(OPEN_DURATION));
      log.warn("Meilisearch unreachable; failing fast for {}", OPEN_DURATION);
    }
    return new SearchTransportException(message, cause);
  }
}
