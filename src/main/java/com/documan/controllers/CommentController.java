// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.dto.request.CreateCommentRequest;
import com.documan.dto.request.UpdateCommentRequest;
import com.documan.dto.response.CommentResponse;
import com.documan.dto.response.PageResponse;
import com.documan.entity.VoteType;
import com.documan.service.CommentService;
import com.documan.service.VoteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comment")
public class CommentController {

  private final CommentService commentService;
  private final VoteService voteService;

  public CommentController(CommentService commentService, VoteService voteService) {
    this.commentService = commentService;
    this.voteService = voteService;
  }

  @GetMapping
  public CommentResponse getComment(@RequestParam("commentId") Integer commentId) {
    return commentService.findById(commentId);
  }

  @GetMapping("/all")
  public PageResponse<CommentResponse> getAllComments(
      @PageableDefault(size = 20, sort = "dateCreated", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return commentService.findAll(pageable);
  }

  @GetMapping("/user")
  public PageResponse<CommentResponse> getCommentsByUser(
      @RequestParam("userId") Integer userId,
      @PageableDefault(size = 20, sort = "dateCreated", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return commentService.findByUser(userId, pageable);
  }

  @GetMapping("/post")
  public PageResponse<CommentResponse> getCommentsByPost(
      @RequestParam("postId") Integer postId,
      @PageableDefault(size = 20, sort = "dateCreated", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return commentService.findByPost(postId, pageable);
  }

  /**
   * The commenting grant, and the named author has to be you.
   *
   * <p>Whether the thread is closed is not asked here — {@code CommentService.create} refuses a
   * closed post already, and that is a state of the post rather than a permission of the caller.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@permissions.isSelf(#userId) and @permissions.mayComment()")
  public CommentResponse createComment(
      @Valid @RequestBody CreateCommentRequest request,
      @RequestParam("userId") Integer userId,
      @RequestParam("postId") Integer postId) {
    return commentService.create(request, userId, postId);
  }

  @PutMapping
  @PreAuthorize("@permissions.mayEditComment(#commentId)")
  public CommentResponse updateComment(
      @Valid @RequestBody UpdateCommentRequest request,
      @RequestParam("commentId") Integer commentId) {
    return commentService.update(commentId, request);
  }

  @PostMapping("/vote")
  @PreAuthorize("@permissions.isSelf(#userId)")
  public CommentResponse voteComment(
      @RequestParam("voteType") VoteType voteType,
      @RequestParam("commentId") Integer commentId,
      @RequestParam("userId") Integer userId) {
    return voteService.voteComment(commentId, userId, voteType);
  }

  @PostMapping("/vote/remove")
  @PreAuthorize("@permissions.isSelf(#userId)")
  public CommentResponse removeVoteComment(
      @RequestParam("voteType") VoteType voteType,
      @RequestParam("commentId") Integer commentId,
      @RequestParam("userId") Integer userId) {
    return voteService.removeVoteComment(commentId, userId, voteType);
  }

  /**
   * The comment's author, or a moderator.
   *
   * <p>Narrower than the client, which also lets the <em>post's</em> author delete comments on their
   * own thread. That is a defensible rule, but it is not this one, and the service has no notion of
   * it — adding it here would mean inventing a rule on the way past. Left as author-or-moderator
   * until someone decides which it should be.
   */
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@permissions.mayEditComment(#commentId)")
  public void deleteComment(@RequestParam("commentId") Integer commentId) {
    commentService.delete(commentId);
  }
}
