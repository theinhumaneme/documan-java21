// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.documan.AbstractDataTest;
import com.documan.dto.response.CommentResponse;
import com.documan.dto.response.PostResponse;
import com.documan.entity.Comment;
import com.documan.entity.Post;
import com.documan.entity.User;
import com.documan.entity.VoteType;
import com.documan.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class VoteServiceTest extends AbstractDataTest {

  @Autowired private VoteService voteService;

  @Test
  void upvoteIncrementsTallyAndRecordsOneRow() {
    User author = newUser("author");
    User voter = newUser("voter");
    Post post = newPost(author, "title");

    PostResponse response = voteService.votePost(post.getId(), voter.getId(), VoteType.UPVOTE);

    assertThat(response.upvoteCount()).isEqualTo(1);
    assertThat(response.downvoteCount()).isZero();
    assertThat(postVoteDao.count()).isEqualTo(1);
  }

  @Test
  void repeatedIdenticalVoteIsIdempotent() {
    User voter = newUser("voter");
    Post post = newPost(newUser("author"), "title");

    voteService.votePost(post.getId(), voter.getId(), VoteType.UPVOTE);
    PostResponse response = voteService.votePost(post.getId(), voter.getId(), VoteType.UPVOTE);

    assertThat(response.upvoteCount()).isEqualTo(1);
    assertThat(postVoteDao.count()).isEqualTo(1);
  }

  @Test
  void switchingDirectionMovesTheTallyAndKeepsASingleRow() {
    User voter = newUser("voter");
    Post post = newPost(newUser("author"), "title");

    voteService.votePost(post.getId(), voter.getId(), VoteType.UPVOTE);
    PostResponse response = voteService.votePost(post.getId(), voter.getId(), VoteType.DOWNVOTE);

    assertThat(response.upvoteCount()).isZero();
    assertThat(response.downvoteCount()).isEqualTo(1);
    assertThat(postVoteDao.count()).isEqualTo(1);
  }

  @Test
  void removingAVoteRestoresTheTally() {
    User voter = newUser("voter");
    Post post = newPost(newUser("author"), "title");

    voteService.votePost(post.getId(), voter.getId(), VoteType.UPVOTE);
    PostResponse response =
        voteService.removeVotePost(post.getId(), voter.getId(), VoteType.UPVOTE);

    assertThat(response.upvoteCount()).isZero();
    assertThat(postVoteDao.count()).isZero();
  }

  @Test
  void removingAVoteThatPointsTheOtherWayChangesNothing() {
    User voter = newUser("voter");
    Post post = newPost(newUser("author"), "title");

    voteService.votePost(post.getId(), voter.getId(), VoteType.UPVOTE);
    PostResponse response =
        voteService.removeVotePost(post.getId(), voter.getId(), VoteType.DOWNVOTE);

    assertThat(response.upvoteCount()).isEqualTo(1);
    assertThat(postVoteDao.count()).isEqualTo(1);
  }

  @Test
  void votesFromDifferentUsersAccumulate() {
    Post post = newPost(newUser("author"), "title");
    User first = newUser("first");
    User second = newUser("second");
    User third = newUser("third");

    voteService.votePost(post.getId(), first.getId(), VoteType.UPVOTE);
    voteService.votePost(post.getId(), second.getId(), VoteType.UPVOTE);
    PostResponse response = voteService.votePost(post.getId(), third.getId(), VoteType.DOWNVOTE);

    assertThat(response.upvoteCount()).isEqualTo(2);
    assertThat(response.downvoteCount()).isEqualTo(1);
    assertThat(postVoteDao.count()).isEqualTo(3);
  }

  @Test
  void commentVotesBehaveTheSameWay() {
    User author = newUser("author");
    User voter = newUser("voter");
    Comment comment = newComment(author, newPost(author, "title"), "body");

    voteService.voteComment(comment.getId(), voter.getId(), VoteType.UPVOTE);
    CommentResponse response =
        voteService.voteComment(comment.getId(), voter.getId(), VoteType.DOWNVOTE);

    assertThat(response.upvoteCount()).isZero();
    assertThat(response.downvoteCount()).isEqualTo(1);
    assertThat(commentVoteDao.count()).isEqualTo(1);
  }

  @Test
  void votingOnAMissingPostReports404() {
    User voter = newUser("voter");

    assertThatThrownBy(() -> voteService.votePost(9999, voter.getId(), VoteType.UPVOTE))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void votingAsAMissingUserReports404() {
    Post post = newPost(newUser("author"), "title");

    assertThatThrownBy(() -> voteService.votePost(post.getId(), 9999, VoteType.UPVOTE))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void netScoreDispatchesOverTheSealedHierarchy() {
    User author = newUser("author");
    Post post = newPost(author, "title");
    Comment comment = newComment(author, post, "body");

    voteService.votePost(post.getId(), newUser("v1").getId(), VoteType.UPVOTE);
    voteService.voteComment(comment.getId(), newUser("v2").getId(), VoteType.DOWNVOTE);

    assertThat(VoteService.netScore(postDao.findById(post.getId()).orElseThrow())).isEqualTo(1);
    assertThat(VoteService.netScore(commentDao.findById(comment.getId()).orElseThrow()))
        .isEqualTo(-1);
  }
}
