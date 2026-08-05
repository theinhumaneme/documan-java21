// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.outbox;

/**
 * Asks for a drain cycle without blocking the caller. Implementations must return immediately even
 * when Meilisearch is slow or unreachable, because this is called from a request thread just after
 * its transaction commits.
 */
public interface DrainTrigger {

  void signal();
}
