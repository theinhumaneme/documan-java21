// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.dto.request.CreateUserRequest;
import com.documan.dto.request.UpdateUserRequest;
import com.documan.dto.response.*;
import com.documan.entity.VoteType;
import com.documan.service.FavouriteService;
import com.documan.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

  private final UserService userService;
  private final FavouriteService favouriteService;

  public UserController(UserService userService, FavouriteService favouriteService) {
    this.userService = userService;
    this.favouriteService = favouriteService;
  }

  @GetMapping
  public UserResponse getUser(@RequestParam("userId") Integer userId) {
    return userService.findById(userId);
  }

  @GetMapping("/username")
  public UserResponse getUserByUsername(@RequestParam("username") String username) {
    return userService.findByUsername(username);
  }

  @GetMapping("/all")
  public PageResponse<UserResponse> getAllUsers(
      @PageableDefault(size = 30, sort = "username") Pageable pageable) {
    return userService.findAll(pageable);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
    return userService.create(request);
  }

  @PutMapping
  public UserResponse updateUser(
      @Valid @RequestBody UpdateUserRequest request, @RequestParam("userId") Integer userId) {
    return userService.update(userId, request);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
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

  @GetMapping("/favourites/posts")
  public PageResponse<PostResponse> getFavouritePosts(
      @RequestParam("userId") Integer userId, @PageableDefault(size = 20) Pageable pageable) {
    return userService.findFavouritePosts(userId, pageable);
  }

  @GetMapping("/favourites/files")
  public PageResponse<FileResponse> getFavouriteFiles(
      @RequestParam("userId") Integer userId, @PageableDefault(size = 20) Pageable pageable) {
    return userService.findFavouriteFiles(userId, pageable);
  }

  @GetMapping("/votes/posts")
  public PageResponse<PostResponse> getVotedPosts(
      @RequestParam("userId") Integer userId,
      @RequestParam("voteType") VoteType voteType,
      @PageableDefault(size = 20) Pageable pageable) {
    return userService.findVotedPosts(userId, voteType, pageable);
  }

  @GetMapping("/votes/comments")
  public PageResponse<CommentResponse> getVotedComments(
      @RequestParam("userId") Integer userId,
      @RequestParam("voteType") VoteType voteType,
      @PageableDefault(size = 20) Pageable pageable) {
    return userService.findVotedComments(userId, voteType, pageable);
  }

  @PostMapping("/favourites/files")
  public FileResponse favouriteFile(
      @RequestParam("fileId") Integer fileId, @RequestParam("userId") Integer userId) {
    return favouriteService.favouriteFile(fileId, userId);
  }

  @PostMapping("/favourites/files/remove")
  public FileResponse removeFavouriteFile(
      @RequestParam("fileId") Integer fileId, @RequestParam("userId") Integer userId) {
    return favouriteService.removeFavouriteFile(fileId, userId);
  }
}
