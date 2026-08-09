// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

/**
 * Entity kinds that are mirrored into Meilisearch. Stored as text in {@code search_outbox}.
 *
 * <p>Files only. Posts, comments and subjects were indexed too, and nothing ever searched them —
 * the one query the application makes is over file names. An index nobody reads still costs a
 * document build and a push on every write to the entity behind it, which for votes is the hottest
 * write path in the service.
 *
 * <p>An enum rather than a constant because the outbox stores the kind as text and the drainer
 * routes on it. A second kind returning is an entry here, a {@link SearchIndex} and a branch in
 * {@code DocumentFactory}.
 */
public enum AggregateType {
  FILE
}
