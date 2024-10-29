// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.PostDao;
import com.kalyan.documan.dao.UserDao;
import com.kalyan.documan.entity.Comment;
import com.kalyan.documan.entity.Post;
import com.kalyan.documan.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PostService {

  private final PostDao postDao;
  private final CommentService commentService;
  private final UserDao userDao;

  @Autowired
  public PostService(PostDao postDao, CommentService commentService, UserDao userDao) {
    this.postDao = postDao;
    this.commentService = commentService;
    this.userDao = userDao;
  }

  public Optional<Post> findById(Integer postId) {
    return postDao.findById(postId);
  }

  public Optional<List<Post>> getAllPosts() {
    return Optional.of(postDao.findAll());
  }

  public Optional<List<Post>> getPostsByUser(Integer userId) {
    return Optional.of(postDao.getPostsByUserId(userId));
  }

  public Optional<List<Comment>> getPostComments(Integer postId) {
    return commentService.getCommentsByPostId(postId);
  }

  public Optional<Post> createPost(Post post, Integer userId) {
    Optional<User> user = userDao.findById(userId);
    if (user.isEmpty()) {
      return Optional.empty();
    } else if (post.getId() != null) {
      return Optional.empty();
    } else {
      Post newPost = new Post();
      newPost.setUser(user.get());
      Post savedPost = postDao.save(newPost);
      return Optional.of(savedPost);
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
      postDao.save(updatedPost);
      return Optional.of(updatedPost);
    }
  }
}
