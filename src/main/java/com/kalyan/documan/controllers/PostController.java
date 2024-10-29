// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.controllers;

import com.kalyan.documan.entity.Post;
import com.kalyan.documan.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/post")
public class PostController {
  private static final Logger logger = LoggerFactory.getLogger(PostController.class);
  private final PostService postService;

  public PostController(PostService postService) {
    this.postService = postService;
  }

  @GetMapping
  public ResponseEntity<?> getPost(@RequestParam("postId") Integer postId) {
    try {
      return postService
          .findById(postId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      logger.error(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @GetMapping("/all")
  public ResponseEntity<?> getAllPosts() {
    try {
      return postService
          .getAllPosts()
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      logger.error(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @PostMapping
  public ResponseEntity<?> createPost(
      @RequestBody Post post, @RequestParam("userId") Integer userId) {
    try {
      return postService
          .createPost(post, userId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      logger.error(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the post.");
    }
  }

  @PutMapping
  public ResponseEntity<?> updatePost(
      @RequestBody Post post, @RequestParam("postId") Integer postId) {
    try {
      return postService
          .updatePost(post, postId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      logger.error(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the post.");
    }
  }
}
