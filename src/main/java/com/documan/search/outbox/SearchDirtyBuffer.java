// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.outbox;

import com.documan.search.AggregateType;
import com.documan.search.SearchProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Collects dirty keys for the duration of a transaction and writes them once, just before commit.
 *
 * <p>Buffering rather than writing inline serves three purposes. The outbox rows are held for the
 * shortest possible time, which matters because concurrent votes on one post contend on a single
 * row. The keys are written in a stable order, so two transactions touching overlapping entity sets
 * take the locks in the same sequence and cannot deadlock. And because the write happens inside the
 * caller's transaction, a failure to enqueue rolls the whole thing back — the only circumstance in
 * which indexing is allowed to fail an application write.
 *
 * <p>The buffer holds a plain resource rather than entities, so the {@code clearAutomatically} bulk
 * counter updates cannot disturb it.
 *
 * <p><strong>A mutating service method must flush before it returns.</strong> Spring calls {@code
 * beforeCommit} below <em>before</em> {@code EntityTransaction.commit()}, and it is that commit
 * which makes Hibernate flush. An entity left to be written at commit therefore raises its
 * {@code @PostUpdate} — and so its dirty key — after this buffer has already written the outbox,
 * and the change is never indexed. Inserts hide this completely: every entity uses IDENTITY
 * generation, so the INSERT and {@code @PostPersist} both happen inside {@code persist()}. The
 * symptom is a newly created row appearing in search correctly while every later edit to it is
 * ignored.
 *
 * <p>Use {@code saveAndFlush} rather than {@code save} (see {@code FileService.rename}), or enqueue
 * explicitly as {@code SubjectService.update} and the vote and favourite services do. Forcing the
 * flush from inside this class is not an option: it would need the {@code EntityManagerFactory},
 * and this bean is constructed by Hibernate's own bean container while that factory is still being
 * built, so asking for it is a circular reference that stops the application from starting.
 */
@Component
public class SearchDirtyBuffer {

  private static final Logger log = LoggerFactory.getLogger(SearchDirtyBuffer.class);
  private static final String RESOURCE_KEY = SearchDirtyBuffer.class.getName();

  private final SearchOutboxStore store;
  private final SearchProperties properties;
  private final ObjectProvider<DrainTrigger> drainTrigger;

  public SearchDirtyBuffer(
      SearchOutboxStore store,
      SearchProperties properties,
      ObjectProvider<DrainTrigger> drainTrigger) {
    this.store = store;
    this.properties = properties;
    this.drainTrigger = drainTrigger;
  }

  /** Marks an aggregate for immediate re-indexing. */
  public void markDirty(AggregateType type, Integer id) {
    enqueue(type, id, Instant.now());
  }

  /**
   * Marks an aggregate whose only change was a vote or favourite tally. Delaying visibility lets a
   * storm of votes on one entity collapse into a single push; a genuine edit arriving meanwhile
   * pulls the key forward, because both this buffer and the upsert keep the earlier of the two
   * times.
   */
  public void markCounterDirty(AggregateType type, Integer id) {
    enqueue(type, id, Instant.now().plus(properties.outbox().counterDebounce()));
  }

  private void enqueue(AggregateType type, Integer id, Instant visibleAt) {
    if (!properties.enabled() || id == null) {
      return;
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      // Outside a transaction there is nothing to be atomic with. Refuse rather than write a row
      // that might describe a change which never lands.
      log.warn("Ignoring dirty {} {} raised outside a transaction", type, id);
      return;
    }
    if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
      return;
    }
    // Earliest visibility wins, so an edit always overrides a debounced counter change.
    pending().merge(new DirtyKey(type, id), visibleAt, (a, b) -> a.isBefore(b) ? a : b);
  }

  @SuppressWarnings("unchecked")
  private SortedMap<DirtyKey, Instant> pending() {
    SortedMap<DirtyKey, Instant> existing =
        (SortedMap<DirtyKey, Instant>) TransactionSynchronizationManager.getResource(RESOURCE_KEY);
    if (existing != null) {
      return existing;
    }
    SortedMap<DirtyKey, Instant> created = new TreeMap<>();
    TransactionSynchronizationManager.bindResource(RESOURCE_KEY, created);
    TransactionSynchronizationManager.registerSynchronization(new FlushOnCommit(created));
    return created;
  }

  private final class FlushOnCommit implements TransactionSynchronization {

    private final SortedMap<DirtyKey, Instant> keys;

    private FlushOnCommit(SortedMap<DirtyKey, Instant> keys) {
      this.keys = keys;
    }

    @Override
    public void beforeCommit(boolean readOnly) {
      if (readOnly || keys.isEmpty()) {
        return;
      }
      // Group by visibility so immediate and debounced keys keep their own times, while each
      // group still writes in key order.
      Map<Instant, List<DirtyKey>> byVisibility = new LinkedHashMap<>();
      keys.forEach(
          (key, visibleAt) ->
              byVisibility.computeIfAbsent(visibleAt, ignored -> new ArrayList<>()).add(key));
      byVisibility.forEach((visibleAt, group) -> store.markDirty(group, visibleAt));
    }

    /**
     * Signalling here rather than through {@code @TransactionalEventListener(AFTER_COMMIT)} is
     * deliberate: that listener runs synchronously on the committing thread, which would put
     * Meilisearch latency and failures onto every write response.
     */
    @Override
    public void afterCommit() {
      drainTrigger.ifAvailable(DrainTrigger::signal);
    }

    @Override
    public void afterCompletion(int status) {
      TransactionSynchronizationManager.unbindResourceIfPossible(RESOURCE_KEY);
    }
  }
}
