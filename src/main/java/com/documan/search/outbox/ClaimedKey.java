// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.outbox;

import com.documan.search.AggregateType;

/**
 * A dirty key taken for processing, carrying the row id and the {@code dirty_seq} observed at claim
 * time. Both are needed for the compare-and-swap delete that follows a successful push.
 */
public record ClaimedKey(long rowId, AggregateType type, Integer aggregateId, long dirtySeq) {

  public DirtyKey key() {
    return new DirtyKey(type, aggregateId);
  }
}
