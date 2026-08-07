// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Creates the indexes and applies their settings.
 *
 * <p>Never blocks or fails startup: Meilisearch being unreachable at boot must degrade to "search
 * is unavailable", not "the application will not start". If the first attempt fails the work is
 * retried lazily before the next push, so the system heals itself once Meilisearch returns.
 */
@Component
@ConditionalOnSearchEnabled
public class IndexBootstrap {

  private static final Logger log = LoggerFactory.getLogger(IndexBootstrap.class);

  private final Client client;
  private final MeilisearchGateway gateway;
  private final SearchProperties properties;
  private final AtomicBoolean applied = new AtomicBoolean();

  public IndexBootstrap(Client client, MeilisearchGateway gateway, SearchProperties properties) {
    this.client = client;
    this.gateway = gateway;
    this.properties = properties;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    ensureApplied();
  }

  /** Idempotent; safe to call before every drain cycle. */
  public boolean ensureApplied() {
    if (applied.get()) {
      return true;
    }
    try {
      for (SearchIndex index : SearchIndex.values()) {
        createIfAbsent(index);
        applySettings(index);
      }
      applied.set(true);
      log.info("Meilisearch indexes ready");
      return true;
    } catch (RuntimeException e) {
      // The whole exception, not just its message. These failures are wrapped several layers deep —
      // a transport problem inside the SDK surfaces here as "Could not update settings for FILES"
      // and nothing else — so dropping the cause leaves no way to tell a misconfigured host from a
      // rejected payload from a missing class, all of which land on this line.
      log.warn("Could not prepare Meilisearch indexes, will retry", e);
      return false;
    }
  }

  private void createIfAbsent(SearchIndex index) {
    String uid = index.uid(properties.indexPrefix());
    try {
      client.createIndex(uid, "id");
    } catch (MeilisearchApiException e) {
      // Only "already exists" is benign; anything else must surface.
      if (!"index_already_exists".equals(e.getCode())) {
        throw new SearchDocumentException("Could not create index " + uid, e);
      }
    } catch (Exception e) {
      throw new SearchTransportException("Could not create index " + uid, e);
    }
  }

  /**
   * Compares before writing, one attribute list at a time.
   *
   * <p>Each list goes through its own endpoint rather than as a single {@code Settings} object. The
   * SDK's {@code Settings} serialises every field it knows about — including {@code
   * filterableAttributesConfig} — and Meilisearch rejects an entire request when it does not
   * recognise a field name, so one blob couples the three settings we actually manage to every
   * field the SDK and the server happen to disagree about. Meilisearch 1.52 rejects that exact
   * field, which killed the call before it left the client and left three of the four indexes
   * uncreated. Sending only the fields we manage cannot break that way.
   *
   * <p>Only the lists that differ are written. Updating a setting makes Meilisearch reindex, so
   * writing all three unconditionally would be three reindexes where one is needed, and writing
   * them on every boot would be a reindex per deploy.
   */
  private void applySettings(SearchIndex index) {
    Index target = gateway.index(index);
    String[] searchable;
    String[] filterable;
    String[] sortable;
    try {
      searchable = target.getSearchableAttributesSettings();
      filterable = target.getFilterableAttributesSettings();
      sortable = target.getSortableAttributesSettings();
    } catch (Exception e) {
      throw new SearchTransportException("Could not read settings for " + index, e);
    }

    try {
      boolean updated = false;
      if (!matches(searchable, index.searchableAttributes())) {
        target.updateSearchableAttributesSettings(index.searchableAttributes());
        updated = true;
      }
      if (!matches(filterable, index.filterableAttributes())) {
        target.updateFilterableAttributesSettings(index.filterableAttributes());
        updated = true;
      }
      if (!matches(sortable, index.sortableAttributes())) {
        target.updateSortableAttributesSettings(index.sortableAttributes());
        updated = true;
      }
      if (updated) {
        log.info("Updated Meilisearch settings for {}", index);
      }
    } catch (Exception e) {
      throw new SearchTransportException("Could not update settings for " + index, e);
    }
  }

  private static boolean matches(Object current, String[] desired) {
    if (!(current instanceof String[] actual)) {
      return false;
    }
    return Arrays.equals(actual, desired);
  }
}
