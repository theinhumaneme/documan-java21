// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.CommentDao;
import com.kalyan.documan.dao.PostDao;
import com.kalyan.documan.dao.UserDao;
import com.kalyan.documan.entity.Comment;
import com.kalyan.documan.entity.Post;
import com.kalyan.documan.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

  private final CommentDao commentDao;
  private final UserDao userDao;
  private final PostDao postDao;

  @Autowired
  public CommentService(CommentDao commentDao, UserDao userDao, PostDao postDao) {
    this.commentDao = commentDao;
    this.userDao = userDao;
    this.postDao = postDao;
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

  public Optional<Comment> addComment(Comment comment, Integer userId, Integer postId) {
    Optional<User> user = userDao.findById(userId);
    Optional<Post> post = postDao.findById(postId);
    if (user.isEmpty() || post.isEmpty()) {
      return Optional.empty();
    } else if (comment.getId() != null) {
      return Optional.empty();
    } else {
      Comment newComment = new Comment();
      newComment.setContent(comment.getContent());
      newComment.setUser(user.get());
      newComment.setPost(post.get());
      Comment savedComment = commentDao.save(newComment);
      return Optional.of(savedComment);
    }
  }

  public Optional<Comment> updateComment(Comment comment, Integer commentId) {
    Optional<Comment> oldComment = commentDao.findById(commentId);
    if (oldComment.isEmpty()) {
      return Optional.empty();
    } else {
      Comment updatedComment = oldComment.get();
      updatedComment.setContent(comment.getContent());
      commentDao.save(updatedComment);
      return Optional.of(updatedComment);
    }
  }
}
