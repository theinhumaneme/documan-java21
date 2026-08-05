// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dao;

import com.documan.entity.Post;
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

public interface PostDao extends JpaRepository<Post, Integer> {

  /**
   * The author is always rendered alongside a post, so it is fetched in the same statement. Without
   * this the list endpoints issued one extra select per row.
   */
  @Override
  @EntityGraph(attributePaths = "user")
  Page<Post> findAll(Pageable pageable);

  @EntityGraph(attributePaths = "user")
  Page<Post> findByUserId(Integer userId, Pageable pageable);

  @EntityGraph(attributePaths = "user")
  Optional<Post> findWithUserById(Integer id);

  /**
   * Applies vote tallies in a single atomic statement. Deliberately not routed through the entity
   * so concurrent votes cannot lose updates and no voter collection is materialised.
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update Post p
         set p.upvoteCount   = p.upvoteCount   + :upDelta,
             p.downvoteCount = p.downvoteCount + :downDelta
       where p.id = :postId
      """)
  void applyVoteDelta(
      @Param("postId") Integer postId,
      @Param("upDelta") long upDelta,
      @Param("downDelta") long downDelta);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("update Post p set p.favouriteCount = p.favouriteCount + :delta where p.id = :postId")
  void applyFavouriteDelta(@Param("postId") Integer postId, @Param("delta") long delta);

  /**
   * Batch load for indexing. {@code findAllById} does not inherit the entity graph declared on the
   * other finders, so without this each row costs an extra select for its author.
   */
  @EntityGraph(attributePaths = "user")
  List<Post> findWithUserByIdIn(Collection<Integer> ids);

  /** Id-only projection used to evict cached read models that embed the author's username. */
  @Query("select p.id from Post p where p.user.id = :userId")
  List<Integer> findIdsByUserId(@Param("userId") Integer userId);
}
