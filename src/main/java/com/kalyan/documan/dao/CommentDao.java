// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.dao;

import com.kalyan.documan.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentDao extends JpaRepository<Comment, Integer> {
  @Query(value = "SELECT * FROM comment where user_id =: userId", nativeQuery = true)
  public List<Comment> getCommentsByUserId(int userId);

  @Query(value = "SELECT * FROM comment where post_id =: postId", nativeQuery = true)
  public List<Comment> getCommentsByPostId(int postId);
}
