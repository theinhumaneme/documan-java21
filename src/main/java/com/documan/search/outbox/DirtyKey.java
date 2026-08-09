// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.outbox;

import com.documan.search.AggregateType;

/** Identifies one aggregate in the dirty set. */
public record DirtyKey(AggregateType type, Integer id) implements Comparable<DirtyKey> {

  /**
   * Buffered keys are written in this order so that two transactions touching overlapping entity
   * sets always take the outbox row locks in the same sequence and cannot deadlock.
   */
  @Override
  public int compareTo(DirtyKey other) {
    int byType = type.compareTo(other.type);
    return byType != 0 ? byType : Integer.compare(id, other.id);
  }
}
