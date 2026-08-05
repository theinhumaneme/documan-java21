// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param enabled master switch. When false the client, drainer and controller are not created and
 *     the capture layer becomes a no-op, so the application runs exactly as it did before search
 *     existed.
 * @param host Meilisearch base URL
 * @param apiKey master key or an index-scoped key
 * @param indexPrefix index uids are global to a Meilisearch instance, so they are namespaced to let
 *     several environments share one server
 * @param requestTimeout ceiling on any single Meilisearch call; an unbounded call is how a search
 *     outage becomes a thread-pool outage
 */
@ConfigurationProperties(prefix = "documan.search")
public record SearchProperties(
    @DefaultValue("false") boolean enabled,
    @DefaultValue("http://localhost:7700") String host,
    @DefaultValue("") String apiKey,
    @DefaultValue("documan_") String indexPrefix,
    @DefaultValue("10s") Duration requestTimeout,
    @DefaultValue Outbox outbox) {

  /**
   * @param batchSize dirty keys claimed per drain cycle
   * @param pollInterval recovery sweep period; the primary trigger is the post-commit signal, so
   *     this only has to catch what a crash or an outage left behind
   * @param maxAttempts document-level failures tolerated before a key stops being claimed
   * @param counterDebounce delay applied when only a vote or favourite tally changed, so a storm of
   *     votes on one entity collapses into a single push
   */
  public record Outbox(
      @DefaultValue("200") int batchSize,
      @DefaultValue("5s") Duration pollInterval,
      @DefaultValue("8") int maxAttempts,
      @DefaultValue("60s") Duration counterDebounce) {}
}
