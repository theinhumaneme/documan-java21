// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.documan.AbstractDataTest;
import com.documan.dto.request.CreateCommentRequest;
import com.documan.dto.request.CreatePostRequest;
import com.documan.dto.request.UpdatePostRequest;
import com.documan.dto.request.UpdateSubjectRequest;
import com.documan.dto.request.UpdateUserRequest;
import com.documan.dto.response.PostResponse;
import com.documan.entity.*;
import com.documan.search.document.CommentDocument;
import com.documan.search.document.FileDocument;
import com.documan.search.document.PostDocument;
import com.documan.search.outbox.SearchOutboxDrainer;
import com.documan.search.outbox.SearchReconcileJob;
import com.documan.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
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
 * <p>Caching runs on Redis here so the username fan-out can be checked against both read models at
 * once — the search index and the cached responses that embed the same denormalised field.
 */
@Testcontainers
@TestPropertySource(properties = {"documan.search.enabled=true", "spring.cache.type=redis"})
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

  @Autowired private PostService postService;
  @Autowired private CommentService commentService;
  @Autowired private SubjectService subjectService;
  @Autowired private UserService userService;
  @Autowired private VoteService voteService;
  @Autowired private FileService fileService;
  @Autowired private SearchService searchService;
  @Autowired private SearchOutboxDrainer drainer;
  @Autowired private IndexBootstrap indexBootstrap;
  @Autowired private CacheManager cacheManager;
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
  void aNewPostBecomesSearchableByTitle() {
    User author = newUser("author");
    postService.create(new CreatePostRequest("Fourier Transforms", "lead", "body"), author.getId());

    drainer.drainOnce();

    assertThat(titlesMatching("Fourier")).contains("Fourier Transforms");
  }

  @Test
  void editingAPostUpdatesItsDocument() {
    User author = newUser("author");
    PostResponse created =
        postService.create(new CreatePostRequest("Original", "lead", "body"), author.getId());
    drainer.drainOnce();

    postService.update(created.id(), new UpdatePostRequest("Rewritten", "lead", "body"));
    drainer.drainOnce();

    assertThat(titlesMatching("Rewritten")).contains("Rewritten");
    assertThat(titlesMatching("Original")).doesNotContain("Original");
  }

  @Test
  void deletingAPostRemovesItsDocument() {
    User author = newUser("author");
    PostResponse created =
        postService.create(new CreatePostRequest("Ephemeral", "lead", "body"), author.getId());
    drainer.drainOnce();
    assertThat(titlesMatching("Ephemeral")).isNotEmpty();

    postService.delete(created.id());
    drainer.drainOnce();

    assertThat(titlesMatching("Ephemeral")).isEmpty();
  }

  /**
   * The comments are removed by a JPA cascade, so {@code CommentService.delete} is never called.
   * Capturing lifecycle events rather than hand-written service hooks is what makes this work.
   */
  @Test
  void deletingAPostAlsoRemovesItsCommentDocuments() {
    User author = newUser("author");
    PostResponse post =
        postService.create(new CreatePostRequest("Thread", "lead", "body"), author.getId());
    commentService.create(
        new CreateCommentRequest("distinctivecommentbody"), author.getId(), post.id());
    drainer.drainOnce();
    assertThat(commentsMatching("distinctivecommentbody")).isNotEmpty();

    postService.delete(post.id());
    drainer.drainOnce();

    assertThat(commentsMatching("distinctivecommentbody")).isEmpty();
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

  /** Fixes both the index and the pre-existing stale-username cache bug in one path. */
  @Test
  void renamingAUserRefreshesTheirPostsInBothTheIndexAndTheCache() {
    User author = newUser("oldname");
    PostResponse post =
        postService.create(new CreatePostRequest("Authored", "lead", "body"), author.getId());
    drainer.drainOnce();

    // Warm the cache so the stale copy would be observable.
    postService.findById(post.id());
    assertThat(cacheManager.getCache("posts").get(post.id(), PostResponse.class).authorUsername())
        .isEqualTo("oldname");

    userService.update(
        author.getId(),
        new UpdateUserRequest(
            "newname",
            null,
            "Test",
            "User",
            "oldname@example.test",
            department.getId(),
            year.getId(),
            semester.getId(),
            null,
            null,
            null));
    drainer.drainOnce();

    assertThat(cacheManager.getCache("posts").get(post.id(), PostResponse.class)).isNull();
    assertThat(postService.findById(post.id()).authorUsername()).isEqualTo("newname");
    assertThat(authorsMatching("Authored")).containsExactly("newname");
  }

  @Test
  void votingUpdatesTheIndexedTally() {
    User author = newUser("author");
    User voter = newUser("voter");
    PostResponse post =
        postService.create(new CreatePostRequest("Votable", "lead", "body"), author.getId());
    drainer.drainOnce();

    voteService.votePost(post.id(), voter.getId(), VoteType.UPVOTE);
    drainer.drainOnce();

    PostDocument document =
        searchService.searchPosts("Votable", null, 0, 10, null).hits().getFirst();
    assertThat(document.upvoteCount()).isEqualTo(1);
    assertThat(document.netScore()).isEqualTo(1);
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

  @Test
  void filtersNarrowResultsToTheRequestedSubject() {
    Subject wanted = newSubject("Wanted Subject", "WS");
    Subject other = newSubject("Other Subject", "OS");
    newFile(wanted, "notes.pdf");
    newFile(other, "notes.pdf");
    drainer.drainOnce();

    var hits =
        searchService.searchFiles(
            "notes", wanted.getId(), null, null, null, null, null, null, 0, 10, null);

    assertThat(hits.hits()).hasSize(1);
    assertThat(hits.hits().getFirst().subjectCode()).isEqualTo("WS");
  }

  /** Nothing that was rolled back may ever reach the index. */
  @Test
  void aRolledBackChangeIsNeverIndexed() {
    User author = newUser("author");
    try {
      postService.create(new CreatePostRequest("Doomed", "lead", null), author.getId());
    } catch (RuntimeException expected) {
      // content is NOT NULL, so the transaction fails
    }
    drainer.drainOnce();

    assertThat(titlesMatching("Doomed")).isEmpty();
  }

  /** The outbox is the durability guarantee: a key survives until it is genuinely indexed. */
  @Test
  void anUndrainedChangeStaysPendingInTheOutbox() {
    User author = newUser("author");
    postService.create(new CreatePostRequest("Pending", "lead", "body"), author.getId());

    Long pending =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM search_outbox WHERE aggregate_type = 'POST'", Long.class);
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
    User author = newUser("author");
    postService.create(new CreatePostRequest("Recoverable", "lead", "body"), author.getId());
    drainer.drainOnce();
    assertThat(titlesMatching("Recoverable")).isNotEmpty();

    // Simulate losing the index without the database ever knowing.
    gateway.deleteAll(SearchIndex.POSTS);
    assertThat(titlesMatching("Recoverable")).isEmpty();

    reconcileJob.reconcile(true);

    assertThat(titlesMatching("Recoverable")).contains("Recoverable");
  }

  /** Every indexed aggregate is covered, and the outbox is left clean afterwards. */
  @Test
  void reconcileEnqueuesEveryAggregateAndDrainsToCompletion() {
    User author = newUser("author");
    Subject subject = newSubject("Reconciled Subject", "RS");
    newFile(subject, "reconciled.pdf");
    PostResponse post =
        postService.create(new CreatePostRequest("Reconciled", "lead", "body"), author.getId());
    commentService.create(new CreateCommentRequest("reconciledcomment"), author.getId(), post.id());
    drainer.drainOnce();

    var enqueued = reconcileJob.reconcile(true);

    assertThat(enqueued)
        .containsOnlyKeys(
            AggregateType.POST, AggregateType.COMMENT, AggregateType.FILE, AggregateType.SUBJECT);
    assertThat(enqueued.values()).allSatisfy(count -> assertThat(count).isEqualTo(1));

    Long remaining = jdbcTemplate.queryForObject("SELECT count(*) FROM search_outbox", Long.class);
    assertThat(remaining).isZero();
    assertThat(titlesMatching("Reconciled")).contains("Reconciled");
    assertThat(commentsMatching("reconciledcomment")).isNotEmpty();
  }

  // ------------------------------------------------------------------ helpers

  private java.util.List<String> titlesMatching(String query) {
    return searchService.searchPosts(query, null, 0, 20, null).hits().stream()
        .map(PostDocument::title)
        .toList();
  }

  private java.util.List<String> authorsMatching(String query) {
    return searchService.searchPosts(query, null, 0, 20, null).hits().stream()
        .map(PostDocument::authorUsername)
        .toList();
  }

  private java.util.List<String> commentsMatching(String query) {
    return searchService.searchComments(query, null, null, 0, 20, null).hits().stream()
        .map(CommentDocument::content)
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
