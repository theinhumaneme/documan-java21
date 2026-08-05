// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dao;

import com.documan.entity.Post;
import com.documan.entity.PostFavourite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostFavouriteDao extends JpaRepository<PostFavourite, Long> {

  Optional<PostFavourite> findByPostIdAndUserId(Integer postId, Integer userId);

  boolean existsByPostIdAndUserId(Integer postId, Integer userId);

  @Query(
      value = "select pf.post from PostFavourite pf where pf.user.id = :userId",
      countQuery = "select count(pf) from PostFavourite pf where pf.user.id = :userId")
  Page<Post> findFavouritePostsByUser(Integer userId, Pageable pageable);

  /** Used to withdraw a departing user's contributions before the database cascade. */
  List<PostFavourite> findByUserId(Integer userId);
}
