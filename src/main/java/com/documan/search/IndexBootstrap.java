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
import com.meilisearch.sdk.model.Settings;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@ConditionalOnBean(Client.class)
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
      log.warn("Could not prepare Meilisearch indexes, will retry: {}", e.getMessage());
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
   * Compares before writing. Updating settings makes Meilisearch reindex the whole index, so
   * pushing them unconditionally on every boot would be a full reindex per deploy the moment the
   * attribute lists stopped being byte-identical.
   */
  private void applySettings(SearchIndex index) {
    Index target = gateway.index(index);
    Settings current;
    try {
      current = target.getSettings();
    } catch (Exception e) {
      throw new SearchTransportException("Could not read settings for " + index, e);
    }

    if (matches(current.getSearchableAttributes(), index.searchableAttributes())
        && matches(current.getFilterableAttributes(), index.filterableAttributes())
        && matches(current.getSortableAttributes(), index.sortableAttributes())) {
      return;
    }

    Settings desired = new Settings();
    desired.setSearchableAttributes(index.searchableAttributes());
    desired.setFilterableAttributes(index.filterableAttributes());
    desired.setSortableAttributes(index.sortableAttributes());
    try {
      target.updateSettings(desired);
      log.info("Updated Meilisearch settings for {}", index);
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
