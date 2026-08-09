// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.dto.request.CreatePostRequest;
import com.documan.dto.request.UpdatePostRequest;
import com.documan.dto.response.PageResponse;
import com.documan.dto.response.PostResponse;
import com.documan.entity.VoteType;
import com.documan.service.FavouriteService;
import com.documan.service.PostService;
import com.documan.service.VoteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/post")
public class PostController {

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
  public PostResponse getPost(@RequestParam("postId") Integer postId) {
    return postService.findById(postId);
  }

  @GetMapping("/all")
  public PageResponse<PostResponse> getAllPosts(
      @PageableDefault(size = 20, sort = "dateCreated", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return postService.findAll(pageable);
  }

  @GetMapping("/user")
  public PageResponse<PostResponse> getPostsByUser(
      @RequestParam("userId") Integer userId,
      @PageableDefault(size = 20, sort = "dateCreated", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return postService.findByUser(userId, pageable);
  }

  /**
   * Writing needs the posting grant, and the named author has to be you.
   *
   * <p>{@code isSelf} is what stops {@code ?userId=} being an impersonation here: the parameter
   * still names the author, but it may now only name the token's owner. Announcing needs an
   * administrator on top, because the flag is taken straight off the request and would otherwise be
   * anyone's to set.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(
      "@permissions.isSelf(#userId) and @permissions.mayPost()"
          + " and (!#request.announcement() or @permissions.mayAnnounce())")
  public PostResponse createPost(
      @Valid @RequestBody CreatePostRequest request, @RequestParam("userId") Integer userId) {
    return postService.create(request, userId);
  }

  /** The author, or a moderator. Changing the announcement flag still needs an administrator. */
  @PutMapping
  @PreAuthorize(
      "@permissions.mayEditPost(#postId)"
          + " and @permissions.mayAnnounceOn(#postId, #request.announcement())")
  public PostResponse updatePost(
      @Valid @RequestBody UpdatePostRequest request, @RequestParam("postId") Integer postId) {
    return postService.update(postId, request);
  }

  // Voting and favouriting need no grant beyond being signed in — they are how a reader reacts, and
  // withholding them is not a moderation tool this application has. What they do need is to be your
  // own: a vote cast in someone else's name is ballot-stuffing wearing their identity.

  @PostMapping("/vote")
  @PreAuthorize("@permissions.isSelf(#userId)")
  public PostResponse votePost(
      @RequestParam("voteType") VoteType voteType,
      @RequestParam("postId") Integer postId,
      @RequestParam("userId") Integer userId) {
    return voteService.votePost(postId, userId, voteType);
  }

  @PostMapping("/vote/remove")
  @PreAuthorize("@permissions.isSelf(#userId)")
  public PostResponse removeVotePost(
      @RequestParam("voteType") VoteType voteType,
      @RequestParam("postId") Integer postId,
      @RequestParam("userId") Integer userId) {
    return voteService.removeVotePost(postId, userId, voteType);
  }

  @PostMapping("/favourite")
  @PreAuthorize("@permissions.isSelf(#userId)")
  public PostResponse favouritePost(
      @RequestParam("postId") Integer postId, @RequestParam("userId") Integer userId) {
    return favouriteService.favouritePost(postId, userId);
  }

  @PostMapping("/favourite/remove")
  @PreAuthorize("@permissions.isSelf(#userId)")
  public PostResponse removeFavouritePost(
      @RequestParam("postId") Integer postId, @RequestParam("userId") Integer userId) {
    return favouriteService.removeFavouritePost(postId, userId);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@permissions.mayEditPost(#postId)")
  public void deletePost(@RequestParam("postId") Integer postId) {
    postService.delete(postId);
  }
}
