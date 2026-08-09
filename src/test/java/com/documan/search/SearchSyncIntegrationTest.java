// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.documan.AbstractDataTest;
import com.documan.dto.request.UpdateSubjectRequest;
import com.documan.entity.*;
import com.documan.search.document.FileDocument;
import com.documan.search.outbox.SearchOutboxDrainer;
import com.documan.search.outbox.SearchReconcileJob;
import com.documan.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end coverage of database-to-index synchronisation.
 *
 * <p>Draining is driven explicitly rather than by the background sweep: a scheduler racing
 * Meilisearch's asynchronous task queue is a reliable way to produce flaky tests. Because the
 * gateway waits for each indexing task to reach a terminal state, a document is guaranteed
 * searchable the moment {@code drainOnce()} returns.
 *
 * <p>Files are the only indexed aggregate, so every case below drives one. The outbox mechanisms
 * being proved — capture, durability, rollback safety and reconcile — are aggregate-agnostic; they
 * were previously demonstrated on posts, which are no longer indexed.
 */
@Testcontainers
// `reconcile.enabled=true` is needed because this class injects SearchReconcileJob, and that bean is
// `@ConditionalOnProperty(matchIfMissing = true)` on exactly this flag — which the shared test
// configuration turns off, so the whole context failed to load rather than one assertion failing.
// Switching it back on here creates the bean without re-enabling the sweep for every other test.
//
// Its cron fires four times a day (`0 0 */6 * * *`), so a scheduled run landing inside a test that
// lasts seconds is not a race worth designing around; the test drives `reconcileJob` directly.
@TestPropertySource(
    properties = {"documan.search.enabled=true", "documan.search.reconcile.enabled=true"})
class SearchSyncIntegrationTest extends AbstractDataTest {

  /** Meilisearch has no Testcontainers module, so it is wired up by hand. */
  @Container
  static final GenericContainer<?> MEILISEARCH =
      new GenericContainer<>("getmeili/meilisearch:v1.52")
          .withExposedPorts(7700)
          .withEnv("MEILI_MASTER_KEY", "test-master-key")
          .withEnv("MEILI_NO_ANALYTICS", "true")
          .waitingFor(Wait.forHttp("/health").forStatusCode(200));

  @DynamicPropertySource
  static void meilisearch(DynamicPropertyRegistry registry) {
    registry.add(
        "documan.search.host",
        () -> "http://%s:%d".formatted(MEILISEARCH.getHost(), MEILISEARCH.getMappedPort(7700)));
    registry.add("documan.search.api-key", () -> "test-master-key");
  }

  @Autowired private SubjectService subjectService;
  @Autowired private SearchService searchService;
  @Autowired private SearchOutboxDrainer drainer;
  @Autowired private IndexBootstrap indexBootstrap;
  @Autowired private MeilisearchGateway gateway;
  @Autowired private SearchReconcileJob reconcileJob;

  @BeforeEach
  void prepareIndexes() {
    assertThat(indexBootstrap.ensureApplied()).isTrue();
    // Documents outlive the database truncation in AbstractDataTest, so the indexes are emptied
    // too; otherwise one case's leftovers satisfy the next case's assertions.
    for (SearchIndex index : SearchIndex.values()) {
      gateway.deleteAll(index);
    }
    drainer.drainOnce();
  }

  @Test
  void aNewFileBecomesSearchableByName() {
    Subject subject = newSubject("Signals", "SIG");
    newFile(subject, "fourier-transforms.pdf");

    drainer.drainOnce();

    assertThat(namesMatching("fourier")).contains("fourier-transforms.pdf");
  }

  /** Every file document copies its subject's name, so a rename has to reach all of them. */
  @Test
  void renamingASubjectRefreshesEveryFileDocument() {
    Subject subject = newSubject("Old Subject Name", "OSN");
    File file = newFile(subject, "handout.pdf");
    drainer.drainOnce();
    assertThat(fileSubjectNames(file.getId())).isEqualTo("Old Subject Name");

    subjectService.update(
        subject.getId(),
        new UpdateSubjectRequest(
            "New Subject Name",
            "OSN",
            false,
            true,
            department.getId(),
            year.getId(),
            semester.getId()));
    drainer.drainOnce();

    assertThat(fileSubjectNames(file.getId())).isEqualTo("New Subject Name");
  }

  /** A subject save that changes nothing searchable must not re-push its files. */
  @Test
  void savingASubjectUnchangedDoesNotFanOut() {
    Subject subject = newSubject("Steady", "STD");
    newFile(subject, "handout.pdf");
    drainer.drainOnce();

    subjectService.update(
        subject.getId(),
        new UpdateSubjectRequest(
            "Steady", "STD", false, true, department.getId(), year.getId(), semester.getId()));

    // Only the subject itself is dirty; no file keys were enqueued.
    Long fileKeys =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM search_outbox WHERE aggregate_type = 'FILE'", Long.class);
    assertThat(fileKeys).isZero();
  }

  /** An uploaded file must be findable, since that is the original point of the feature. */
  @Test
  void uploadedFilesAreSearchable() {
    Subject subject = newSubject("Digital Design", "DD");
    newFile(subject, "verilog-primer.pdf");
    drainer.drainOnce();

    var hits =
        searchService.searchFiles("verilog", null, null, null, null, null, null, null, 0, 10, null);

    assertThat(hits.hits()).extracting(FileDocument::name).contains("verilog-primer.pdf");
    assertThat(hits.hits().getFirst().extension()).isEqualTo("pdf");
  }

  /** Nothing that was rolled back may ever reach the index. */
  @Test
  void aRolledBackChangeIsNeverIndexed() {
    Subject subject = newSubject("Signals", "SIG");
    try {
      File doomed = new File();
      doomed.setName("doomed.pdf");
      doomed.setObjectName("obj-doomed.pdf");
      doomed.setObjectURL("http://localhost/obj-doomed.pdf");
      doomed.setSize(null); // size is NOT NULL, so the transaction fails
      doomed.setSubject(subject);
      fileDao.saveAndFlush(doomed);
    } catch (RuntimeException expected) {
      // the insert is rejected and the capture must not survive it
    }
    drainer.drainOnce();

    assertThat(namesMatching("doomed")).isEmpty();
  }

  /** The outbox is the durability guarantee: a key survives until it is genuinely indexed. */
  @Test
  void anUndrainedChangeStaysPendingInTheOutbox() {
    Subject subject = newSubject("Signals", "SIG");
    newFile(subject, "pending.pdf");

    Long pending =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM search_outbox WHERE aggregate_type = 'FILE'", Long.class);
    assertThat(pending).isEqualTo(1);

    drainer.drainOnce();

    Long remaining = jdbcTemplate.queryForObject("SELECT count(*) FROM search_outbox", Long.class);
    assertThat(remaining).isZero();
  }

  /**
   * The reconcile job is also the bootstrap path: an index emptied behind the application's back
   * must refill without anyone touching the rows.
   */
  @Test
  void reconcileRebuildsAnIndexThatWasEmptiedOutOfBand() {
    Subject subject = newSubject("Signals", "SIG");
    newFile(subject, "recoverable.pdf");
    drainer.drainOnce();
    assertThat(namesMatching("recoverable")).isNotEmpty();

    // Simulate losing the index without the database ever knowing.
    gateway.deleteAll(SearchIndex.FILES);
    assertThat(namesMatching("recoverable")).isEmpty();

    reconcileJob.reconcile(true);

    assertThat(namesMatching("recoverable")).contains("recoverable.pdf");
  }

  /** The indexed aggregate is covered, and the outbox is left clean afterwards. */
  @Test
  void reconcileEnqueuesEveryAggregateAndDrainsToCompletion() {
    Subject subject = newSubject("Reconciled Subject", "RS");
    newFile(subject, "reconciled.pdf");
    drainer.drainOnce();

    var enqueued = reconcileJob.reconcile(true);

    assertThat(enqueued).containsOnlyKeys(AggregateType.FILE);
    assertThat(enqueued.values()).allSatisfy(count -> assertThat(count).isEqualTo(1));

    Long remaining = jdbcTemplate.queryForObject("SELECT count(*) FROM search_outbox", Long.class);
    assertThat(remaining).isZero();
    assertThat(namesMatching("reconciled")).contains("reconciled.pdf");
  }

  // ------------------------------------------------------------------ helpers

  private java.util.List<String> namesMatching(String query) {
    return searchService
        .searchFiles(query, null, null, null, null, null, null, null, 0, 20, null)
        .hits()
        .stream()
        .map(FileDocument::name)
        .toList();
  }

  private String fileSubjectNames(Integer fileId) {
    return searchService
        .searchFiles(null, null, null, null, null, null, null, null, 0, 50, null)
        .hits()
        .stream()
        .filter(document -> document.id().equals(fileId))
        .map(FileDocument::subjectName)
        .findFirst()
        .orElse(null);
  }
}
