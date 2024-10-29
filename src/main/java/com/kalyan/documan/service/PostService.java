// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.PostDao;
import com.kalyan.documan.entity.Comment;
import com.kalyan.documan.entity.Post;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PostService {

  private final PostDao postDao;
  private final CommentService commentService;

  @Autowired
  public PostService(PostDao postDao, CommentService commentService) {
    this.postDao = postDao;
    this.commentService = commentService;
  }

  public Optional<Post> getPost(Integer postId) {
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
}
