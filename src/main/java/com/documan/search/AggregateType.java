// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

/** Entity kinds that are mirrored into Meilisearch. Stored as text in {@code search_outbox}. */
public enum AggregateType {
  POST,
  COMMENT,
  FILE,
  SUBJECT
}
