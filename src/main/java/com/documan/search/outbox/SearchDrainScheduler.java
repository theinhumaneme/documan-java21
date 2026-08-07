// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.outbox;

import com.documan.search.ConditionalOnSearchEnabled;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The two things that start a drain cycle.
 *
 * <p>{@link #signal()} is called from a transaction's {@code afterCommit}, so it must return
 * immediately — it hands off to a single-threaded executor and never touches the network on the
 * caller's thread. This is why the post-commit hook is not a
 * {@code @TransactionalEventListener(AFTER_COMMIT)}: that runs synchronously on the committing
 * thread and would put Meilisearch latency, and Meilisearch failures, into every write response.
 *
 * <p>The scheduled sweep is the durability half. It uses {@code fixedDelay} rather than {@code
 * fixedRate} so cycles cannot pile up behind a slow Meilisearch, and it is what recovers anything a
 * crash or an outage left in the outbox.
 */
@Component
@ConditionalOnSearchEnabled
@ConditionalOnProperty(
    prefix = "documan.search.drainer",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SearchDrainScheduler implements DrainTrigger {

  private static final Logger log = LoggerFactory.getLogger(SearchDrainScheduler.class);

  private final SearchOutboxDrainer drainer;
  private final AtomicBoolean scheduled = new AtomicBoolean();
  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(Thread.ofVirtual().name("search-drain").factory());

  public SearchDrainScheduler(SearchOutboxDrainer drainer) {
    this.drainer = drainer;
  }

  /** Non-blocking. Coalesces: if a drain is already queued, this is a no-op. */
  @Override
  public void signal() {
    if (!scheduled.compareAndSet(false, true)) {
      return;
    }
    try {
      executor.execute(
          () -> {
            scheduled.set(false);
            runQuietly();
          });
    } catch (RejectedExecutionException e) {
      // Shutting down; the scheduled sweep on the next start will pick the work up.
      scheduled.set(false);
    }
  }

  @Scheduled(
      fixedDelayString = "${documan.search.outbox.poll-interval:5s}",
      initialDelayString = "${documan.search.outbox.poll-interval:5s}")
  void sweep() {
    runQuietly();
  }

  private void runQuietly() {
    try {
      int cleared = drainer.drain();
      if (cleared > 0) {
        log.debug("Indexed {} dirty key(s)", cleared);
      }
    } catch (RuntimeException e) {
      // Never let a drain failure escape: it must not kill the scheduler or surface to a caller.
      log.error("Search drain cycle failed", e);
    }
  }
}
