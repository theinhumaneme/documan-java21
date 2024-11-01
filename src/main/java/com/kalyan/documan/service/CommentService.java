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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

  private static final Logger log = LoggerFactory.getLogger(CommentService.class);
  private final CommentDao commentDao;
  private final UserDao userDao;
  private final PostDao postDao;
  private final RedisCacheService redisCacheService;

  @Autowired
  public CommentService(
      CommentDao commentDao,
      UserDao userDao,
      PostDao postDao,
      RedisCacheService redisCacheService) {
    this.commentDao = commentDao;
    this.userDao = userDao;
    this.postDao = postDao;
    this.redisCacheService = redisCacheService;
  }

  public Optional<Comment> getComment(Integer commentId) {
    String commentKey = String.format("COMMENT%s", commentId);
    Optional<Comment> cachedEntity = redisCacheService.getValue(commentKey, Comment.class);
    if (cachedEntity.isEmpty()) {
      log.error("Comment {} not found in cache", commentId);
      Optional<Comment> comment = commentDao.findById(commentId);
      if (comment.isPresent()) {
        Optional<Comment> cachedComment = redisCacheService.setValue(commentKey, comment.get());
        if (cachedComment.isEmpty()) {
          log.error("Failed to cache Comment {}", commentId);
        } else {
          log.info("cached Comment {}", commentId);
        }
        return comment; // return user from db cache if exists
      } else {
        return Optional.empty();
      }
    } else {
      log.info("Comment {} found in cache", commentId);
    }
    return cachedEntity;
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
      String commentKey = String.format("COMMENT%s", savedComment.getId());
      Optional<Comment> cachedEntity = redisCacheService.updateValue(commentKey, savedComment);
      if (cachedEntity.isEmpty()) {
        log.error("Failed to add Comment {} in cache", savedComment.getId());
      } else {
        log.info("Comment {} added in cache", savedComment.getId());
      }
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
      Comment updatedEntity = commentDao.save(updatedComment);
      String commentKey = String.format("COMMENT%s", updatedEntity.getId());
      Optional<Comment> cachedEntity = redisCacheService.updateValue(commentKey, updatedEntity);
      if (cachedEntity.isEmpty()) {
        log.error("Failed to update Comment {} in cache", updatedEntity.getId());
      } else {
        log.info("Comment {} updated in cache", updatedEntity.getId());
      }
      return Optional.of(updatedComment);
    }
  }

  public Optional<Comment> deleteComment(Integer commentId) {
    Optional<Comment> oldComment = commentDao.findById(commentId);
    if (oldComment.isEmpty()) {
      return Optional.empty();
    }
    Comment deletedComment = oldComment.get();
    commentDao.delete(oldComment.get());
    String commentKey = String.format("COMMENT%s", deletedComment.getId());
    log.info("Comment {} deletion from cache started", deletedComment.getId());
    redisCacheService.deleteValue(commentKey);
    log.info("Comment {} deleted from cache ", deletedComment.getId());
    return Optional.of(deletedComment);
  }
}
