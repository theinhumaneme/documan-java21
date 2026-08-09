// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.outbox;

import com.documan.search.AggregateType;
import com.documan.search.SearchProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * All SQL against {@code search_outbox}.
 *
 * <p>Uses plain JDBC rather than a JPA repository on purpose: the upsert needs {@code ON CONFLICT},
 * the fan-outs are set-based, and keeping the queue out of the persistence context means the {@code
 * clearAutomatically = true} bulk counter updates can never interact with it.
 */
@Component
public class SearchOutboxStore {

  /**
   * {@code least(...)} means a re-dirty can only ever pull a key's visibility forward, so a real
   * edit arriving during a counter debounce is not delayed by it.
   *
   * <p>The {@code attempts} expression gives a fresh change a fresh retry budget, but refuses to
   * resurrect a key that has already exhausted it — otherwise a permanently broken document would
   * retry forever every time someone voted on it.
   */
  private static final String UPSERT =
      """
      INSERT INTO search_outbox (aggregate_type, aggregate_id, visible_at, dirty_seq,
                                 attempts, first_seen_at)
           VALUES (?, ?, ?, 1, 0, ?)
      ON CONFLICT (aggregate_type, aggregate_id) DO UPDATE
              SET visible_at = LEAST(EXCLUDED.visible_at, search_outbox.visible_at),
                  dirty_seq  = search_outbox.dirty_seq + 1,
                  attempts   = CASE WHEN search_outbox.attempts >= %d
                                    THEN search_outbox.attempts ELSE 0 END
      """;

  private final JdbcClient jdbcClient;
  private final JdbcTemplate jdbcTemplate;
  private final SearchProperties properties;

  public SearchOutboxStore(
      JdbcClient jdbcClient, JdbcTemplate jdbcTemplate, SearchProperties properties) {
    this.jdbcClient = jdbcClient;
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties;
  }

  /** Joins the caller's transaction, so enqueue and the entity change commit together. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void markDirty(Collection<DirtyKey> keys, Instant visibleAt) {
    if (keys.isEmpty()) {
      return;
    }
    Timestamp visible = Timestamp.from(visibleAt);
    Timestamp now = Timestamp.from(Instant.now());
    List<DirtyKey> ordered = keys.stream().sorted().toList();

    jdbcTemplate.batchUpdate(
        upsertSql(),
        ordered,
        ordered.size(),
        (ps, key) -> {
          ps.setString(1, key.type().name());
          ps.setInt(2, key.id());
          ps.setTimestamp(3, visible);
          ps.setTimestamp(4, now);
        });
  }

  // ------------------------------------------------------------------ fan-out
  //
  // Set-based rather than reading ids into the JVM and inserting them one by one. Each SELECT
  // yields primary keys, so the result is distinct and ON CONFLICT DO UPDATE cannot hit the same
  // row twice within one statement.

  @Transactional(propagation = Propagation.MANDATORY)
  public int markFilesOfSubjectDirty(Integer subjectId) {
    return fanOut("FILE", "SELECT f.id FROM file f WHERE f.subject_id = ?", subjectId);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public int markPostsOfUserDirty(Integer userId) {
    return fanOut("POST", "SELECT p.id FROM post p WHERE p.user_id = ?", userId);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public int markCommentsOfUserDirty(Integer userId) {
    return fanOut("COMMENT", "SELECT c.id FROM comment c WHERE c.user_id = ?", userId);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public int markCommentsOfPostDirty(Integer postId) {
    return fanOut("COMMENT", "SELECT c.id FROM comment c WHERE c.post_id = ?", postId);
  }

  private int fanOut(String type, String idSelect, Integer parameter) {
    String sql =
        """
        INSERT INTO search_outbox (aggregate_type, aggregate_id, visible_at, dirty_seq,
                                   attempts, first_seen_at)
             SELECT '%s', src.id, now(), 1, 0, now() FROM (%s) AS src
        ON CONFLICT (aggregate_type, aggregate_id) DO UPDATE
                SET visible_at = LEAST(EXCLUDED.visible_at, search_outbox.visible_at),
                    dirty_seq  = search_outbox.dirty_seq + 1,
                    attempts   = CASE WHEN search_outbox.attempts >= %d
                                      THEN search_outbox.attempts ELSE 0 END
        """
            .formatted(type, idSelect, properties.outbox().maxAttempts());
    return jdbcTemplate.update(sql, parameter);
  }

  // ------------------------------------------------------------------ drainer side

  /**
   * No locking clause, and a short read-only transaction: the drainer must not hold a connection
   * open across the Meilisearch round trip. Overlapping claims are made safe by the
   * compare-and-swap in {@link #clearProcessed} rather than by locking rows here.
   */
  @Transactional(readOnly = true)
  public List<ClaimedKey> claim(int batchSize) {
    return jdbcClient
        .sql(
            """
            SELECT id, aggregate_type, aggregate_id, dirty_seq
              FROM search_outbox
             WHERE visible_at <= now() AND attempts < :maxAttempts
             ORDER BY visible_at, id
             LIMIT :batchSize
            """)
        .param("maxAttempts", properties.outbox().maxAttempts())
        .param("batchSize", batchSize)
        .query(
            (rs, row) ->
                new ClaimedKey(
                    rs.getLong("id"),
                    AggregateType.valueOf(rs.getString("aggregate_type")),
                    rs.getInt("aggregate_id"),
                    rs.getLong("dirty_seq")))
        .list();
  }

  /**
   * Compare-and-swap. Deleting by key instead would discard a change committed while the push was
   * in flight; matching on {@code dirty_seq} leaves such a key dirty so the next cycle retries it.
   */
  @Transactional
  public int clearProcessed(List<ClaimedKey> processed) {
    if (processed.isEmpty()) {
      return 0;
    }
    // The batched overload reports one result array per batch, hence the nested loop.
    int[][] affected =
        jdbcTemplate.batchUpdate(
            "DELETE FROM search_outbox WHERE id = ? AND dirty_seq = ?",
            processed,
            processed.size(),
            (ps, claimed) -> {
              ps.setLong(1, claimed.rowId());
              ps.setLong(2, claimed.dirtySeq());
            });
    int total = 0;
    for (int[] batch : affected) {
      for (int count : batch) {
        total += Math.max(count, 0);
      }
    }
    return total;
  }

  /**
   * Records a document-level failure with exponential backoff. Transport failures must not come
   * through here — an outage would otherwise drive the whole backlog past {@code maxAttempts} and
   * dead-letter it just before Meilisearch came back.
   */
  @Transactional
  public void recordFailure(List<ClaimedKey> failed, String error) {
    if (failed.isEmpty()) {
      return;
    }
    String truncated = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
    jdbcTemplate.batchUpdate(
        """
        UPDATE search_outbox
           SET attempts   = attempts + 1,
               visible_at = now() + (interval '1 second'
                            * LEAST(300, POWER(2, attempts)::int)),
               last_error = ?
         WHERE id = ?
        """,
        failed,
        failed.size(),
        (ps, claimed) -> {
          ps.setString(1, truncated);
          ps.setLong(2, claimed.rowId());
        });
  }

  // ------------------------------------------------------------------ reconcile

  /** Stable key for the reconcile leader lock. Arbitrary, but must not collide with other users. */
  private static final long RECONCILE_LOCK_KEY = 0x0D0C_5EA7_C400_0002L;

  /**
   * Marks every indexed row dirty, in one short transaction, if this instance wins the lock.
   *
   * <p>{@code pg_try_advisory_xact_lock} rather than a session lock: it is released automatically
   * when this transaction ends, so it cannot be stranded on a pooled connection, and the whole
   * enqueue is a handful of set-based statements that finish in milliseconds.
   *
   * <p>Unlike the incremental path this resets {@code attempts}, so a key that previously exhausted
   * its retry budget gets another chance on each scheduled run rather than staying dead forever.
   *
   * @return rows enqueued per aggregate, or an empty map if another instance holds the lock
   */
  @Transactional
  public Map<AggregateType, Integer> markAllDirtyIfLeader() {
    Boolean acquired =
        jdbcTemplate.queryForObject(
            "SELECT pg_try_advisory_xact_lock(?)", Boolean.class, RECONCILE_LOCK_KEY);
    if (!Boolean.TRUE.equals(acquired)) {
      return Map.of();
    }

    Map<AggregateType, Integer> enqueued = new EnumMap<>(AggregateType.class);
    enqueued.put(AggregateType.FILE, reconcileAll("FILE", "SELECT f.id FROM file f"));
    return enqueued;
  }

  private int reconcileAll(String type, String idSelect) {
    String sql =
        """
        INSERT INTO search_outbox (aggregate_type, aggregate_id, visible_at, dirty_seq,
                                   attempts, first_seen_at)
             SELECT '%s', src.id, now(), 1, 0, now() FROM (%s) AS src
        ON CONFLICT (aggregate_type, aggregate_id) DO UPDATE
                SET visible_at = LEAST(EXCLUDED.visible_at, search_outbox.visible_at),
                    dirty_seq  = search_outbox.dirty_seq + 1,
                    attempts   = 0
        """
            .formatted(type, idSelect);
    return jdbcTemplate.update(sql);
  }

  // ------------------------------------------------------------------ observability

  @Transactional(readOnly = true)
  public long depth() {
    return jdbcClient
        .sql("SELECT count(*) FROM search_outbox WHERE attempts < :max")
        .param("max", properties.outbox().maxAttempts())
        .query(Long.class)
        .single();
  }

  @Transactional(readOnly = true)
  public long deadCount() {
    return jdbcClient
        .sql("SELECT count(*) FROM search_outbox WHERE attempts >= :max")
        .param("max", properties.outbox().maxAttempts())
        .query(Long.class)
        .single();
  }

  /** Age of the oldest pending key in seconds — the real staleness signal to alert on. */
  @Transactional(readOnly = true)
  public long oldestPendingAgeSeconds() {
    Long age =
        jdbcClient
            .sql(
                """
                SELECT COALESCE(EXTRACT(EPOCH FROM (now() - min(first_seen_at)))::bigint, 0)
                  FROM search_outbox WHERE attempts < :max
                """)
            .param("max", properties.outbox().maxAttempts())
            .query(Long.class)
            .single();
    return age == null ? 0L : age;
  }

  private String upsertSql() {
    return UPSERT.formatted(properties.outbox().maxAttempts());
  }
}
