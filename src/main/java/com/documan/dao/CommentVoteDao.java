// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dao;

import com.documan.entity.Comment;
import com.documan.entity.CommentVote;
import com.documan.entity.VoteType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentVoteDao extends JpaRepository<CommentVote, Long> {

  Optional<CommentVote> findByCommentIdAndUserId(Integer commentId, Integer userId);

  @Query(
      value =
          "select cv.comment from CommentVote cv where cv.user.id = :userId and cv.voteType = :type",
      countQuery =
          "select count(cv) from CommentVote cv where cv.user.id = :userId and cv.voteType = :type")
  Page<Comment> findVotedCommentsByUser(Integer userId, VoteType type, Pageable pageable);

  /** Used to withdraw a departing user's contributions before the database cascade. */
  List<CommentVote> findByUserId(Integer userId);
}
