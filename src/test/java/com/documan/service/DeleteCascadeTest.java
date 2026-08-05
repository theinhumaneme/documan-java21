// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.documan.AbstractDataTest;
import com.documan.entity.Post;
import com.documan.entity.User;
import com.documan.entity.VoteType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DeleteCascadeTest extends AbstractDataTest {

  @Autowired private PostService postService;
  @Autowired private VoteService voteService;
  @Autowired private FavouriteService favouriteService;

  @Test
  void deletingAPostThatHasVotesAndFavouritesSucceeds() {
    User author = newUser("author");
    User voter = newUser("voter");
    Post post = newPost(author, "title");
    newComment(author, post, "a comment");

    voteService.votePost(post.getId(), voter.getId(), VoteType.UPVOTE);
    favouriteService.favouritePost(post.getId(), voter.getId());

    assertThatCode(() -> postService.delete(post.getId())).doesNotThrowAnyException();

    assertThat(postDao.count()).isZero();
    assertThat(postVoteDao.count()).isZero();
    assertThat(postFavouriteDao.count()).isZero();
    assertThat(commentDao.count()).isZero();
  }

  @Test
  void deletingACommentThatHasVotesSucceeds() {
    User author = newUser("author");
    User voter = newUser("voter");
    Post post = newPost(author, "title");
    var comment = newComment(author, post, "a comment");

    voteService.voteComment(comment.getId(), voter.getId(), VoteType.UPVOTE);

    assertThatCode(() -> postService.delete(post.getId())).doesNotThrowAnyException();

    assertThat(commentVoteDao.count()).isZero();
  }
}
