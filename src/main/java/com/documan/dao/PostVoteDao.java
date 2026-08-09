// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dao;

import com.documan.entity.Post;
import com.documan.entity.PostVote;
import com.documan.entity.VoteType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostVoteDao extends JpaRepository<PostVote, Long> {

  Optional<PostVote> findByPostIdAndUserId(Integer postId, Integer userId);

  @Query(
      value = "select pv.post from PostVote pv where pv.user.id = :userId and pv.voteType = :type",
      countQuery =
          "select count(pv) from PostVote pv where pv.user.id = :userId and pv.voteType = :type")
  Page<Post> findVotedPostsByUser(Integer userId, VoteType type, Pageable pageable);

  /** Used to withdraw a departing user's contributions before the database cascade. */
  List<PostVote> findByUserId(Integer userId);
}
