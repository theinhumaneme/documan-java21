// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.outbox;

import com.documan.dao.FileDao;
import com.documan.search.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Moves dirty keys into Meilisearch.
 *
 * <p>The cycle deliberately never holds a database transaction across the HTTP call. Claiming with
 * {@code FOR UPDATE SKIP LOCKED} and deleting on success would pin a connection from a pool of 50
 * for the whole of any Meilisearch stall — turning one outage into two — and it locks rows rather
 * than keys, so it would not even guarantee that a given entity is handled by one worker at a time.
 *
 * <p>Concurrency is handled in two layers instead. Within a process a plain lock keeps the
 * scheduled sweep and the post-commit signal from overlapping. Across replicas nothing coordinates,
 * and it does not need to: the compare-and-swap in {@link SearchOutboxStore#clearProcessed} means
 * that if two workers race and one of them pushes a document that was deleted meanwhile, neither
 * clears the key, and the next cycle reads the row as absent and removes the document. The system
 * converges rather than requiring distributed mutual exclusion, which also avoids having to pin an
 * advisory lock to a specific pooled connection.
 *
 * <p>Because one worker submits sequentially and Meilisearch processes tasks per index in FIFO
 * order, submission order matches application order without extra machinery.
 */
@Component
@ConditionalOnSearchEnabled
public class SearchOutboxDrainer {

  private static final Logger log = LoggerFactory.getLogger(SearchOutboxDrainer.class);

  private final ReentrantLock cycleLock = new ReentrantLock();

  private final SearchOutboxStore store;
  private final MeilisearchGateway gateway;
  private final DocumentFactory documents;
  private final ObjectMapper objectMapper;
  private final SearchProperties properties;
  private final FileDao fileDao;

  public SearchOutboxDrainer(
      SearchOutboxStore store,
      MeilisearchGateway gateway,
      DocumentFactory documents,
      ObjectMapper objectMapper,
      SearchProperties properties,
      FileDao fileDao) {
    this.store = store;
    this.gateway = gateway;
    this.documents = documents;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.fileDao = fileDao;
  }

  /**
   * Runs cycles until the outbox is drained or a cycle makes no progress. Returns the number of
   * keys cleared. Skips entirely if another cycle is already running in this process.
   */
  public int drain() {
    if (!cycleLock.tryLock()) {
      return 0;
    }
    try {
      int total = 0;
      int cleared;
      do {
        cleared = drainOnce();
        total += cleared;
      } while (cleared >= properties.outbox().batchSize());
      return total;
    } finally {
      cycleLock.unlock();
    }
  }

  /** One claim-push-clear pass. Exposed for tests, which drive draining explicitly. */
  public int drainOnce() {
    List<ClaimedKey> claimed = store.claim(properties.outbox().batchSize());
    if (claimed.isEmpty()) {
      return 0;
    }

    Map<AggregateType, List<ClaimedKey>> byType = new LinkedHashMap<>();
    for (ClaimedKey key : claimed) {
      byType.computeIfAbsent(key.type(), ignored -> new ArrayList<>()).add(key);
    }

    List<ClaimedKey> succeeded = new ArrayList<>();
    for (Map.Entry<AggregateType, List<ClaimedKey>> entry : byType.entrySet()) {
      try {
        pushGroup(entry.getKey(), entry.getValue());
        succeeded.addAll(entry.getValue());
      } catch (SearchTransportException e) {
        // Availability problem: leave every remaining key untouched and let the next cycle retry.
        // Consuming the retry budget here would dead-letter the whole backlog during an outage,
        // exactly when it must survive.
        log.warn("Search indexing paused, Meilisearch unreachable: {}", e.getMessage());
        break;
      } catch (SearchDocumentException e) {
        log.warn("Search indexing rejected {} batch: {}", entry.getKey(), e.getMessage());
        store.recordFailure(entry.getValue(), e.getMessage());
      }
    }

    int cleared = store.clearProcessed(succeeded);
    if (cleared < succeeded.size()) {
      // The difference is keys re-dirtied while the push was in flight; they stay pending so the
      // newer state is what eventually reaches the index.
      log.debug("{} key(s) were re-dirtied during indexing", succeeded.size() - cleared);
    }
    return cleared;
  }

  /**
   * Loads the current state of a group of keys and reconciles the index to it.
   *
   * <p>Why the key carries no operation: presence at read time is the entire decision. A key whose
   * row has since been deleted — whether it was marked dirty by a delete, or by an update that a
   * delete then overtook — is removed from the index. Anything else leaves a tombstone that search
   * would return and a click would 404 on.
   *
   * <p>This relies on identity columns never recycling ids, so "a row with id 42 exists" implies it
   * is the same entity 42 that was marked dirty.
   */
  private void pushGroup(AggregateType type, List<ClaimedKey> keys) {
    Set<Integer> ids = new HashSet<>();
    keys.forEach(key -> ids.add(key.aggregateId()));

    Set<Integer> present = new HashSet<>();
    List<?> upserts =
        switch (type) {
          case FILE ->
              map(
                  fileDao.findForIndexingByIdIn(ids),
                  f -> f.getId(),
                  present,
                  documents::toDocument);
        };

    SearchIndex index = SearchIndex.of(type);
    if (!upserts.isEmpty()) {
      gateway.upsert(index, objectMapper.writeValueAsString(upserts));
    }

    List<String> removed =
        ids.stream().filter(id -> !present.contains(id)).map(String::valueOf).toList();
    if (!removed.isEmpty()) {
      gateway.delete(index, removed);
    }
  }

  private <E, D> List<D> map(
      List<E> entities,
      Function<E, Integer> idOf,
      Set<Integer> presentOut,
      Function<E, D> toDocument) {
    List<D> mapped = new ArrayList<>(entities.size());
    for (E entity : entities) {
      presentOut.add(idOf.apply(entity));
      mapped.add(toDocument.apply(entity));
    }
    return mapped;
  }
}
