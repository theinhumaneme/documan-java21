// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.PostDao;
import com.kalyan.documan.dao.UserDao;
import com.kalyan.documan.entity.Post;
import com.kalyan.documan.entity.User;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostService {
  private static final Logger log = LoggerFactory.getLogger(PostService.class);
  private final PostDao postDao;
  private final CommentService commentService;
  private final UserDao userDao;
  private final RedisCacheService redisCacheService;

  @Autowired
  public PostService(
      PostDao postDao,
      CommentService commentService,
      UserDao userDao,
      RedisCacheService redisCacheService) {
    this.postDao = postDao;
    this.commentService = commentService;
    this.userDao = userDao;
    this.redisCacheService = redisCacheService;
  }

  public Optional<Post> findById(Integer postId) {
    String postKey = String.format("POST%s", postId);
    Optional<Post> cachedEntity = redisCacheService.getValue(postKey, Post.class);
    if (cachedEntity.isEmpty()) {
      log.error("Post {} not found in cache", postId);
      Optional<Post> post = postDao.findById(postId);
      if (post.isPresent()) {
        Optional<Post> cachedPost = redisCacheService.setValue(postKey, post.get());
        if (cachedPost.isEmpty()) {
          log.error("Failed to cache Post {}", postId);
        } else {
          log.info("cached Post {}", postId);
        }
        return post; // return user from db cache if exists
      } else {
        return Optional.empty();
      }
    } else {
      log.info("Post {} found in cache", postId);
    }
    return cachedEntity;
  }

  public Optional<List<Post>> getAllPosts() {
    return Optional.of(postDao.findAll());
  }

  public Optional<List<Post>> getPostsByUser(Integer userId) {
    return Optional.of(postDao.getPostsByUserId(userId));
  }

  public Optional<Post> createPost(Post post, Integer userId) {
    Optional<User> user = userDao.findById(userId);
    if (user.isEmpty()) {
      return Optional.empty();
    } else if (post.getId() != null) {
      return Optional.empty();
    } else {
      Post newPost = new Post();
      newPost.setTitle(post.getTitle());
      newPost.setContent(post.getContent());
      newPost.setDescription(post.getDescription());
      newPost.setUser(user.get());
      Post updatedEntity = postDao.save(newPost);
      String commentKey = String.format("POST%s", updatedEntity.getId());
      Optional<Post> cachedEntity = redisCacheService.updateValue(commentKey, updatedEntity);
      if (cachedEntity.isEmpty()) {
        log.error("Failed to add Post {} in cache", updatedEntity.getId());
      } else {
        log.info("Post {} add in cache", updatedEntity.getId());
      }
      return Optional.of(updatedEntity);
    }
  }

  public Optional<Post> updatePost(Post post, Integer postId) {
    Optional<Post> oldPost = postDao.findById(postId);
    if (oldPost.isEmpty()) {
      return Optional.empty();
    } else {
      Post updatedPost = oldPost.get();
      updatedPost.setContent(post.getContent());
      updatedPost.setTitle(post.getTitle());
      updatedPost.setDescription(post.getDescription());
      Post updatedEntity = postDao.save(updatedPost);
      String commentKey = String.format("POST%s", updatedEntity.getId());
      Optional<Post> cachedEntity = redisCacheService.updateValue(commentKey, updatedEntity);
      if (cachedEntity.isEmpty()) {
        log.error("Failed to update Post {} in cache", updatedEntity.getId());
      } else {
        log.info("Post {} updated in cache", updatedEntity.getId());
      }
      return Optional.of(updatedEntity);
    }
  }

  public Optional<Post> deletePost(Integer postId) {
    Optional<Post> oldPost = postDao.findById(postId);
    if (oldPost.isEmpty()) {
      return Optional.empty();
    }
    Post deletedComment = oldPost.get();
    postDao.delete(oldPost.get());
    String commentKey = String.format("COMMENT%s", deletedComment.getId());
    log.info("Comment {} deletion from cache started", deletedComment.getId());
    redisCacheService.deleteValue(commentKey);
    log.info("Comment {} deleted from cache ", deletedComment.getId());
    return Optional.of(deletedComment);
  }
}
