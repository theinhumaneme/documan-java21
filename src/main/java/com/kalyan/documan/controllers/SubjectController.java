// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SubjectController {
  private static final Logger logger = LoggerFactory.getLogger(SubjectController.class);

  @GetMapping("/api/v1/subject")
  public ResponseEntity<?> getSubject(@RequestParam("subjectId") Long subjectId) {
    try {
      if (subjectId != null) {
        return ResponseEntity.status(HttpStatus.OK).body("Here is your Subject");
      } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Expected parameter subjectId");
      }
    } catch (Exception e) {
      logger.error(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @PostMapping("/api/v1/subject")
  public ResponseEntity<?> createSubject(@RequestParam("subject") Long subjectId) {
    try {
      if (subjectId == null) {
        return ResponseEntity.status(HttpStatus.OK).body(("Expected Subject, received none"));
      } else {
        return ResponseEntity.status(HttpStatus.OK).body("Here is your Subject");
      }
    } catch (Exception e) {
      logger.error(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the subject.");
    }
  }
}
