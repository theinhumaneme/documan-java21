// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.dto.request.CreateUserRequest;
import com.documan.dto.request.UpdatePermissionsRequest;
import com.documan.dto.request.UpdateProfileRequest;
import com.documan.dto.request.UpdateUserRequest;
import com.documan.dto.response.*;
import com.documan.entity.VoteType;
import com.documan.security.CurrentUser;
import com.documan.service.FavouriteService;
import com.documan.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

  private final UserService userService;
  private final FavouriteService favouriteService;
  private final CurrentUser currentUser;

  public UserController(
      UserService userService, FavouriteService favouriteService, CurrentUser currentUser) {
    this.userService = userService;
    this.favouriteService = favouriteService;
    this.currentUser = currentUser;
  }

  /**
   * Who the bearer token belongs to, creating the row on first sign-in.
   *
   * <p>The client's first call after Clerk returns it a token, and the only endpoint that turns a
   * directory identity into something the rest of the API can reference. 401 when the request
   * carries no token — a {@code GET} is otherwise open here, because reading does not need one.
   */
  @GetMapping("/me")
  public UserResponse getCurrentUser() {
    return userService.findById(currentUser.requireId());
  }

  /**
   * File yourself against a department, year and semester, and accept the terms.
   *
   * <p>Takes no {@code userId}: it acts on whoever the token says is asking, which is the only
   * sensible reading of "my profile" and the shape every endpoint here is headed for.
   */
  @PutMapping("/me/profile")
  public UserResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateProfile(currentUser.requireId(), request);
  }

  /**
   * One person's row. Yourself, or anyone if you moderate.
   *
   * <p>A {@code UserResponse} carries an email address and the account's permissions, so this is a
   * directory lookup rather than a public profile. Nothing in the client calls it — a post already
   * carries its {@code authorUsername}, which is what a reader actually needs.
   */
  @GetMapping
  @PreAuthorize("@permissions.isSelfOrModerator(#userId)")
  public UserResponse getUser(@RequestParam("userId") Integer userId) {
    return userService.findById(userId);
  }

  /** The same lookup by name. Moderators only: there is no id here to compare against. */
  @GetMapping("/username")
  @PreAuthorize("@permissions.isModerator()")
  public UserResponse getUserByUsername(@RequestParam("username") String username) {
    return userService.findByUsername(username);
  }

  /**
   * The whole directory — names, addresses, permissions. Moderators and above.
   *
   * <p>The filter chain already refuses this anonymously, which is why it did not leak to the world.
   * It did leak to every signed-in reader, and only the console ever asks for it.
   */
  @GetMapping("/all")
  @PreAuthorize("@permissions.isModerator()")
  public PageResponse<UserResponse> getAllUsers(
      @PageableDefault(size = 30, sort = "username") Pageable pageable) {
    return userService.findAll(pageable);
  }

  /**
   * The old sign-up form's endpoint, kept behind an administrator.
   *
   * <p>Clerk creates accounts now and {@link com.documan.security.CurrentUser} provisions the row on
   * first token, so nothing in the client calls this. Left in place rather than deleted because a
   * row created here has no {@code external_id} and is therefore claimable by its owner's first
   * sign-in — the same path that carries the seeded administrator across — which makes it the only
   * way to pre-create an account for someone who has not signed in yet.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@permissions.isAdmin()")
  public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
    return userService.create(request);
  }

  /** Your own row, or anyone's if you administer. */
  @PutMapping
  @PreAuthorize("@permissions.isSelfOrAdmin(#userId)")
  public UserResponse updateUser(
      @Valid @RequestBody UpdateUserRequest request, @RequestParam("userId") Integer userId) {
    return userService.update(userId, request);
  }

  /**
   * Grant or withdraw posting, commenting or verification.
   *
   * <p>Separate from the profile update above because the console changes one fact at a time and
   * because a profile update demands a department, year and semester that a newly provisioned
   * account does not have. See {@link UpdatePermissionsRequest}.
   */
  @PutMapping("/permissions")
  @PreAuthorize("@permissions.isModerator()")
  public UserResponse updatePermissions(
      @RequestBody UpdatePermissionsRequest request, @RequestParam("userId") Integer userId) {
    return userService.updatePermissions(userId, request);
  }

  /**
   * Administrators only, and not yourself.
   *
   * <p>Self-deletion is refused because this row is the far end of every post, comment and vote the
   * person wrote, and because an administrator who deletes their own row while being the only one
   * leaves an installation nobody can administer. Removing your own account is a Clerk action with a
   * different shape — it has to decide what happens to the writing — and it does not exist yet.
   */
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@permissions.isAdmin() and !@permissions.isSelf(#userId)")
  public void deleteUser(@RequestParam("userId") Integer userId) {
    userService.delete(userId);
  }

  // Relationship views. These service methods existed but had no route.

  @GetMapping("/posts")
  public PageResponse<PostResponse> getUserPosts(
      @RequestParam("userId") Integer userId,
      @PageableDefault(size = 20, sort = "dateCreated", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return userService.findPosts(userId, pageable);
  }

  @GetMapping("/comments")
  public PageResponse<CommentResponse> getUserComments(
      @RequestParam("userId") Integer userId,
      @PageableDefault(size = 20, sort = "dateCreated", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return userService.findComments(userId, pageable);
  }

  @GetMapping("/subjects")
  public PageResponse<SubjectResponse> getUserSubjects(
      @RequestParam("userId") Integer userId,
      @PageableDefault(size = 20, sort = "name") Pageable pageable) {
    return userService.findSubjects(userId, pageable);
  }

  // Favourites and votes are the reader's own record of what they liked and how they voted. A
  // signed-in stranger could read either before these annotations, which is a per-person browsing
  // history — and a vote is meant to be private in a way a post is not. The client only ever asks
  // about itself; a moderator may ask about anyone, because moderation is what the answer is for.

  @GetMapping("/favourites/posts")
  @PreAuthorize("@permissions.isSelfOrModerator(#userId)")
  public PageResponse<PostResponse> getFavouritePosts(
      @RequestParam("userId") Integer userId, @PageableDefault(size = 20) Pageable pageable) {
    return userService.findFavouritePosts(userId, pageable);
  }

  @GetMapping("/favourites/files")
  @PreAuthorize("@permissions.isSelfOrModerator(#userId)")
  public PageResponse<FileResponse> getFavouriteFiles(
      @RequestParam("userId") Integer userId, @PageableDefault(size = 20) Pageable pageable) {
    return userService.findFavouriteFiles(userId, pageable);
  }

  @GetMapping("/votes/posts")
  @PreAuthorize("@permissions.isSelfOrModerator(#userId)")
  public PageResponse<PostResponse> getVotedPosts(
      @RequestParam("userId") Integer userId,
      @RequestParam("voteType") VoteType voteType,
      @PageableDefault(size = 20) Pageable pageable) {
    return userService.findVotedPosts(userId, voteType, pageable);
  }

  @GetMapping("/votes/comments")
  @PreAuthorize("@permissions.isSelfOrModerator(#userId)")
  public PageResponse<CommentResponse> getVotedComments(
      @RequestParam("userId") Integer userId,
      @RequestParam("voteType") VoteType voteType,
      @PageableDefault(size = 20) Pageable pageable) {
    return userService.findVotedComments(userId, voteType, pageable);
  }

  // Writing a favourite is yourself only. There is no moderator exception: bookmarking on someone
  // else's behalf is not a moderation act, it is impersonation.

  @PostMapping("/favourites/files")
  @PreAuthorize("@permissions.isSelf(#userId)")
  public FileResponse favouriteFile(
      @RequestParam("fileId") Integer fileId, @RequestParam("userId") Integer userId) {
    return favouriteService.favouriteFile(fileId, userId);
  }

  @PostMapping("/favourites/files/remove")
  @PreAuthorize("@permissions.isSelf(#userId)")
  public FileResponse removeFavouriteFile(
      @RequestParam("fileId") Integer fileId, @RequestParam("userId") Integer userId) {
    return favouriteService.removeFavouriteFile(fileId, userId);
  }
}
