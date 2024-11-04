// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.controllers;

import com.kalyan.documan.entity.Post;
import com.kalyan.documan.service.FavouriteService;
import com.kalyan.documan.service.PostService;
import com.kalyan.documan.service.VoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/post")
public class PostController {
  private static final Logger log = LoggerFactory.getLogger(PostController.class);
  private final PostService postService;
  private final VoteService voteService;
  private final FavouriteService favouriteService;

  public PostController(
      PostService postService, VoteService voteService, FavouriteService favouriteService) {
    this.postService = postService;
    this.voteService = voteService;
    this.favouriteService = favouriteService;
  }

  @GetMapping
  public ResponseEntity<?> getPost(@RequestParam("postId") Integer postId) {
    try {
      return postService
          .findById(postId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      log.error(e.toString());
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
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @GetMapping("/user")
  public ResponseEntity<?> getPostsByUser(@RequestParam("userId") Integer userId) {
    try {
      return postService
          .getPostsByUser(userId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      log.error(e.toString());
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
      log.error(e.toString());
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
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the post.");
    }
  }

  @PostMapping("/vote")
  public ResponseEntity<?> votePost(
      @RequestParam("voteType") String voteType,
      @RequestParam("postId") Integer postId,
      @RequestParam("userId") Integer userId) {
    try {
      return voteService
          .votePost(userId, postId, voteType)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while applying the vote");
    }
  }

  @PostMapping("/vote/remove")
  public ResponseEntity<?> removeVotePost(
      @RequestParam("voteType") String voteType,
      @RequestParam("postId") Integer postId,
      @RequestParam("userId") Integer userId) {
    try {
      return voteService
          .removeVotePost(userId, postId, voteType)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while applying the vote");
    }
  }

  @PostMapping("/favourite")
  public ResponseEntity<?> favouritePost(
      @RequestParam("postId") Integer postId, @RequestParam("userId") Integer userId) {
    try {
      return favouriteService
          .favouritePost(postId, userId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while applying the vote");
    }
  }

  @PostMapping("/favourite/remove")
  public ResponseEntity<?> removeFavouritePost(
      @RequestParam("postId") Integer postId, @RequestParam("userId") Integer userId) {
    try {
      return favouriteService
          .removeFavouritePost(postId, userId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while applying the vote");
    }
  }

  @DeleteMapping()
  public ResponseEntity<?> deleteComment(@RequestParam(value = "postId") Integer postId) {
    try {
      return postService
          .deletePost(postId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }
}
