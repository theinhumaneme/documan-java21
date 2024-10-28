// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.CommentDao;
import com.kalyan.documan.entity.Comment;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class CommentService {

  private final CommentDao commentDao;

  @Autowired
  public CommentService(CommentDao commentDao) {
    this.commentDao = commentDao;
  }

  public Optional<Comment> getComment(Integer commentId) {
    return commentDao.findById(commentId);
  }

  public Optional<List<Comment>> getAllComments() {
    return Optional.of(commentDao.findAll());
  }

  public Optional<List<Comment>> getCommentsByUserId(Integer userId) {
    List<Comment> comments = commentDao.getCommentsByUserId(userId);
    if (!comments.isEmpty()) {
      return Optional.of(comments);
    }
    return Optional.empty();
  }

  public Optional<List<Comment>> getCommentsByPostId(Integer postId) {
    List<Comment> comments = commentDao.getCommentsByPostId(postId);
    if (!comments.isEmpty()) {
      return Optional.of(comments);
    }
    return Optional.empty();
  }
}
