// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.outbox;

import com.documan.search.AggregateType;
import com.documan.search.ConditionalOnSearchEnabled;
import com.documan.search.IndexBootstrap;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically marks every indexed row dirty, so the index converges on the database even if
 * something was missed.
 *
 * <p>Deliberately does not talk to Meilisearch. It only enqueues, and the drainer does the pushing
 * — which means it inherits the existing batching, retry, failure classification and backpressure
 * for free, and cannot itself stall on a slow index. The enqueue is four set-based statements, so
 * the job's own cost is milliseconds regardless of table size; the real work is spread across
 * subsequent drain cycles.
 *
 * <p>It is an upsert, not a rebuild. Clearing the indexes first would make search return nothing
 * until the rebuild finished; re-pushing over a live index keeps it serving throughout.
 *
 * <p>This doubles as the bootstrap path: pointing the application at a populated database and an
 * empty Meilisearch, then triggering a reconcile, fills the index.
 *
 * <p>What it cannot detect: a document that is present but stale for a reason the outbox never saw
 * — an out-of-band {@code UPDATE}, say. Re-pushing everything is precisely how that is repaired,
 * which is the point of running it on a schedule rather than only diffing what is missing.
 */
@Component
@ConditionalOnSearchEnabled
@ConditionalOnProperty(
    prefix = "documan.search.reconcile",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SearchReconcileJob {

  private static final Logger log = LoggerFactory.getLogger(SearchReconcileJob.class);

  private final SearchOutboxStore store;
  private final SearchOutboxDrainer drainer;
  private final IndexBootstrap indexBootstrap;

  public SearchReconcileJob(
      SearchOutboxStore store, SearchOutboxDrainer drainer, IndexBootstrap indexBootstrap) {
    this.store = store;
    this.drainer = drainer;
    this.indexBootstrap = indexBootstrap;
  }

  @Scheduled(cron = "${documan.search.reconcile.cron:0 0 */6 * * *}")
  void scheduled() {
    try {
      reconcile();
    } catch (RuntimeException e) {
      // A failed reconcile must not kill the scheduler; the next run tries again.
      log.error("Search reconcile failed", e);
    }
  }

  /**
   * Enqueues every indexed row and drains. Returns the number of keys enqueued per aggregate.
   *
   * @param drainInline when true the caller waits for the push to finish, which is what a manual
   *     bootstrap wants; the scheduled run just signals and lets the drainer proceed on its own
   */
  public Map<AggregateType, Integer> reconcile(boolean drainInline) {
    if (!indexBootstrap.ensureApplied()) {
      log.warn("Skipping reconcile: Meilisearch indexes are not ready");
      return Map.of();
    }

    Instant startedAt = Instant.now();
    // One replica does the enqueue. The others skip rather than duplicating four full-table
    // inserts; the work itself is idempotent, so this is about avoiding waste, not correctness.
    Map<AggregateType, Integer> enqueued = store.markAllDirtyIfLeader();
    if (enqueued.isEmpty()) {
      log.debug("Reconcile skipped, another instance holds the lock");
      return enqueued;
    }

    int total = enqueued.values().stream().mapToInt(Integer::intValue).sum();
    log.info(
        "Reconcile enqueued {} key(s) in {}ms: {}",
        total,
        Duration.between(startedAt, Instant.now()).toMillis(),
        enqueued);

    if (drainInline) {
      int cleared = drainer.drain();
      log.info("Reconcile indexed {} key(s)", cleared);
    }
    return enqueued;
  }

  private void reconcile() {
    reconcile(true);
  }
}
