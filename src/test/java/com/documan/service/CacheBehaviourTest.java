// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.documan.AbstractDataTest;
import com.documan.dto.response.PostResponse;
import com.documan.dto.response.UserResponse;
import com.documan.entity.Post;
import com.documan.entity.User;
import com.documan.entity.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

/**
 * Runs against an in-memory cache manager so the
 * {@code @Cacheable}/{@code @CachePut}/{@code @CacheEvict} wiring and its SpEL key expressions are
 * verified without needing Redis. The Redis-specific serialisation path is covered by {@code
 * ContainerizedSmokeTest}.
 */
@TestPropertySource(properties = "spring.cache.type=simple")
class CacheBehaviourTest extends AbstractDataTest {

  @Autowired private PostService postService;
  @Autowired private UserService userService;
  @Autowired private VoteService voteService;
  @Autowired private CacheManager cacheManager;

  @BeforeEach
  void clearCaches() {
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
  }

  @Test
  void readingAPostPopulatesTheCache() {
    Post post = newPost(newUser("author"), "title");

    postService.findById(post.getId());

    assertThat(cached("posts", post.getId(), PostResponse.class)).isNotNull();
  }

  /** Exercises the {@code #result.id()} key expression on create. */
  @Test
  void creatingAUserCachesItUnderItsGeneratedId() {
    UserResponse created =
        userService.create(
            new com.documan.dto.request.CreateUserRequest(
                "fresh",
                "initial-password",
                "First",
                "Last",
                "fresh@example.test",
                department.getId(),
                year.getId(),
                semester.getId(),
                true));

    assertThat(cached("users", created.id(), UserResponse.class)).isNotNull();
  }

  @Test
  void updatingAPostReplacesTheCachedEntry() {
    Post post = newPost(newUser("author"), "before");
    postService.findById(post.getId());

    postService.update(
        post.getId(),
        new com.documan.dto.request.UpdatePostRequest("after", "d", "c", false, false));

    assertThat(cached("posts", post.getId(), PostResponse.class).title()).isEqualTo("after");
  }

  @Test
  void deletingAPostEvictsItsOwnEntry() {
    Post post = newPost(newUser("author"), "title");
    postService.findById(post.getId());

    postService.delete(post.getId());

    assertThat(cached("posts", post.getId(), PostResponse.class)).isNull();
  }

  /**
   * The previous implementation evicted {@code COMMENT{id}} when deleting a post, leaving the post
   * cached and potentially dropping an unrelated comment with the same numeric id.
   */
  @Test
  void deletingAPostDoesNotDisturbACommentWithTheSameId() {
    User author = newUser("author");
    Post post = newPost(author, "title");
    var comment = newComment(author, post, "body");

    postService.findById(post.getId());
    var cachedComment = cacheManager.getCache("comments");
    cachedComment.put(post.getId(), "sentinel");

    postService.delete(post.getId());

    assertThat(cachedComment.get(post.getId())).isNotNull();
    assertThat(cached("posts", post.getId(), PostResponse.class)).isNull();
    assertThat(comment.getId()).isNotNull();
  }

  /** A vote changes the tallies, so the stale cached post must go. */
  @Test
  void votingEvictsTheCachedPost() {
    Post post = newPost(newUser("author"), "title");
    User voter = newUser("voter");
    postService.findById(post.getId());
    assertThat(cached("posts", post.getId(), PostResponse.class).upvoteCount()).isZero();

    voteService.votePost(post.getId(), voter.getId(), VoteType.UPVOTE);

    assertThat(cached("posts", post.getId(), PostResponse.class)).isNull();
    assertThat(postService.findById(post.getId()).upvoteCount()).isEqualTo(1);
  }

  private <T> T cached(String cache, Object key, Class<T> type) {
    return cacheManager.getCache(cache).get(key, type);
  }
}
