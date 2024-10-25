// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.controllers;

import com.kalyan.documan.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {
  private static final Logger logger = LoggerFactory.getLogger(UserController.class);
  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/api/v1/user/id")
  public ResponseEntity<?> getUser(
      @RequestParam(value = "userId", required = true) Integer userId) {
    try {
      return userService
          .findById(userId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      logger.error(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @GetMapping("/api/v1/user/username")
  public ResponseEntity<?> getUser(@RequestParam("username") String username) {
    try {
      return userService
          .findByUsername(username)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      logger.error(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @PostMapping("/api/v1/user")
  public ResponseEntity<?> createUser(@RequestParam("user") Long userId) {
    try {
      if (userId == null) {
        return ResponseEntity.status(HttpStatus.OK).body(("Expected User, received none"));
      } else {
        return ResponseEntity.status(HttpStatus.OK).body("Here is your User");
      }
    } catch (Exception e) {
      logger.error(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the user.");
    }
  }
}
