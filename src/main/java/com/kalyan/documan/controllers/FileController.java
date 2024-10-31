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
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileController {
  private static final Logger log = LoggerFactory.getLogger(FileController.class);

  @GetMapping("/api/v1/file")
  public ResponseEntity<?> getFile(@RequestParam("fileId") Long fileId) {
    try {
      if (fileId != null) {
        return ResponseEntity.status(HttpStatus.OK).body("Here is your File");
      } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Expected parameter fileId");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @PostMapping("/api/v1/file")
  public ResponseEntity<?> createFile(@RequestParam("file") MultipartFile file) {
    try {
      if (file == null) {
        return ResponseEntity.status(HttpStatus.OK).body(("Expected File, received none"));
      } else {
        return ResponseEntity.status(HttpStatus.OK).body("Here is your File");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the file.");
    }
  }
}
