// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.outbox;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * The dirty set: at most one row per aggregate, meaning "this entity may be out of sync".
 *
 * <p>It is a set rather than a log because the drainer re-reads current state from the database, so
 * a recorded operation would carry no information — presence at read time decides upsert versus
 * delete. That keeps the table bounded by the number of distinct entities instead of by write
 * volume, which matters because voting is the hottest write path in the service.
 *
 * <p>This class exists so {@code ddl-auto} and {@code SchemaExportTest} generate the table. All
 * runtime access goes through {@link SearchOutboxStore} on {@code JdbcClient}: the queue must stay
 * out of the persistence context, and the upsert needs {@code ON CONFLICT}, which JPA cannot
 * express.
 *
 * <p>Deliberately has no {@code @Version}. Every other entity here does, but concurrent votes on
 * one post contend on a single outbox row, and optimistic locking would surface that as a failed
 * user write.
 */
@Entity
@Table(
    name = "search_outbox",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_search_outbox_key",
            columnNames = {"aggregate_type", "aggregate_id"}),
    indexes = @Index(name = "idx_search_outbox_visible_at", columnList = "visible_at, id"))
@Getter
@Setter
public class SearchOutboxEntry {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "aggregate_type", nullable = false, length = 16)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private Integer aggregateId;

  /** When the key becomes claimable. Carries both retry backoff and the counter debounce. */
  @Column(name = "visible_at", nullable = false)
  private OffsetDateTime visibleAt;

  /**
   * Bumped on every re-dirty. The drainer deletes with {@code AND dirty_seq = ?}, so a change
   * committed while a push was in flight is retried rather than silently dropped.
   */
  @Column(name = "dirty_seq", nullable = false, columnDefinition = "bigint default 1")
  private long dirtySeq;

  /** Only document-level failures count; transport failures must not exhaust the budget. */
  @Column(name = "attempts", nullable = false, columnDefinition = "integer default 0")
  private int attempts;

  @Column(name = "last_error", length = 1000)
  private String lastError;

  @Column(name = "first_seen_at", nullable = false)
  private OffsetDateTime firstSeenAt;
}
