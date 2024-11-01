// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.controllers;

import com.kalyan.documan.entity.Comment;
import com.kalyan.documan.service.CommentService;
import com.kalyan.documan.service.VoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(("/api/v1/comment"))
public class CommentController {
  private static final Logger log = LoggerFactory.getLogger(CommentController.class);
  private final CommentService commentService;
  private final VoteService voteService;

  public CommentController(CommentService commentService, VoteService voteService) {
    this.commentService = commentService;
    this.voteService = voteService;
  }

  @GetMapping
  public ResponseEntity<?> getComment(@RequestParam("commentId") Integer commentId) {
    try {
      return commentService
          .getComment(commentId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @GetMapping("/all")
  public ResponseEntity<?> getAllComments() {
    try {
      return commentService
          .getAllComments()
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @PostMapping
  public ResponseEntity<?> createComment(
      @RequestBody Comment comment,
      @RequestParam("userId") Integer userId,
      @RequestParam("postId") Integer postId) {
    try {
      return commentService
          .addComment(comment, userId, postId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the comment.");
    }
  }

  @PutMapping
  public ResponseEntity<?> createComment(
      @RequestBody Comment comment, @RequestParam("commentId") Integer commentId) {
    try {
      return commentService
          .updateComment(comment, commentId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the comment.");
    }
  }

  @PostMapping("vote")
  public ResponseEntity<?> voteComment(
      @RequestParam("voteType") String voteType,
      @RequestParam("commentId") Integer commentId,
      @RequestParam("userId") Integer userId) {
    try {
      return voteService
          .voteCommment(userId, commentId, voteType)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while applying the vote");
    }
  }

  @DeleteMapping()
  public ResponseEntity<?> deleteComment(@RequestParam(value = "commentId") Integer commentId) {
    try {
      return commentService
          .deleteComment(commentId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }
}
