// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan;

import static org.assertj.core.api.Assertions.assertThat;

import com.documan.dto.response.PostResponse;
import com.documan.dto.response.UserResponse;
import com.documan.entity.Post;
import com.documan.entity.User;
import com.documan.service.PostService;
import com.documan.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

/**
 * The one thing the rest of the suite cannot cover: values actually surviving a round trip through
 * Redis and the per-cache typed JSON serializers. Everywhere else caching is switched off, so a
 * serialisation fault would go unnoticed until production.
 */
@TestPropertySource(properties = "spring.cache.type=redis")
class RedisCacheSerializationTest extends AbstractDataTest {

  @Autowired private UserService userService;
  @Autowired private PostService postService;
  @Autowired private CacheManager cacheManager;

  @Test
  void userResponsesRoundTripThroughRedis() {
    User user = newUser("cached");

    UserResponse first = userService.findById(user.getId());
    UserResponse cached = cacheManager.getCache("users").get(user.getId(), UserResponse.class);

    assertThat(cached).isNotNull();
    assertThat(cached.username()).isEqualTo(first.username());
    assertThat(cached.department().name()).isEqualTo(first.department().name());
    // Timestamps are the field most likely to break under a serializer change.
    assertThat(cached.dateCreated()).isEqualTo(first.dateCreated());
  }

  @Test
  void postResponsesRoundTripThroughRedis() {
    Post post = newPost(newUser("poster"), "title");

    PostResponse first = postService.findById(post.getId());
    PostResponse cached = cacheManager.getCache("posts").get(post.getId(), PostResponse.class);

    assertThat(cached).isNotNull();
    assertThat(cached.title()).isEqualTo(first.title());
    assertThat(cached.authorUsername()).isEqualTo("poster");
    assertThat(cached.dateModified()).isEqualTo(first.dateModified());
  }
}
