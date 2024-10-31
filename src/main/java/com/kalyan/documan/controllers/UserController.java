// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.controllers;

import com.kalyan.documan.entity.User;
import com.kalyan.documan.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(("/api/v1/user"))
public class UserController {
  private static final Logger log = LoggerFactory.getLogger(UserController.class);
  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/id")
  public ResponseEntity<?> getUser(
      @RequestParam(value = "userId", required = true) Integer userId) {
    try {
      return userService
          .findById(userId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @GetMapping("/username")
  public ResponseEntity<?> getUser(@RequestParam("username") String username) {
    try {
      return userService
          .findByUsername(username)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @PostMapping()
  public ResponseEntity<?> createUser(
      @RequestBody User user,
      @RequestParam("departmentId") Integer departmentId,
      @RequestParam("yearId") Integer yearId,
      @RequestParam("semesterId") Integer semesterId) {
    try {
      return userService
          .createUser(user, departmentId, yearId, semesterId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the user.");
    }
  }

  @PutMapping()
  public ResponseEntity<?> updateUser(
      @RequestBody User user,
      @RequestParam("userId") Integer userId,
      @RequestParam("departmentId") Integer departmentId,
      @RequestParam("yearId") Integer yearId,
      @RequestParam("semesterId") Integer semesterId) {
    try {
      return userService
          .updateUser(user, userId, departmentId, yearId, semesterId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the user.");
    }
  }
}
