// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import java.util.List;

/**
 * Search results.
 *
 * <p>{@code estimatedTotalHits} is Meilisearch's own wording and is deliberately not renamed to
 * "total": the engine trades an exact count for speed, so callers must not paginate as though it
 * were authoritative.
 */
public record SearchResponse<T>(
    List<T> hits,
    String query,
    int page,
    int size,
    long estimatedTotalHits,
    long processingTimeMs) {}
