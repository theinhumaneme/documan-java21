// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dao;

import com.documan.entity.Comment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentDao extends JpaRepository<Comment, Integer> {

  @Override
  @EntityGraph(attributePaths = "user")
  Page<Comment> findAll(Pageable pageable);

  @EntityGraph(attributePaths = "user")
  Page<Comment> findByUserId(Integer userId, Pageable pageable);

  @EntityGraph(attributePaths = "user")
  Page<Comment> findByPostId(Integer postId, Pageable pageable);

  @EntityGraph(attributePaths = {"user", "post"})
  Optional<Comment> findWithUserById(Integer id);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update Comment c
         set c.upvoteCount   = c.upvoteCount   + :upDelta,
             c.downvoteCount = c.downvoteCount + :downDelta
       where c.id = :commentId
      """)
  void applyVoteDelta(
      @Param("commentId") Integer commentId,
      @Param("upDelta") long upDelta,
      @Param("downDelta") long downDelta);

  /** Batch load for indexing; {@code post} is needed for the document's postId. */
  @EntityGraph(attributePaths = {"user", "post"})
  List<Comment> findWithUserByIdIn(Collection<Integer> ids);

  /** Id-only projection used to evict cached read models that embed the author's username. */
  @Query("select c.id from Comment c where c.user.id = :userId")
  List<Integer> findIdsByUserId(@Param("userId") Integer userId);
}
