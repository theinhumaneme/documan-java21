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
  private static final Logger logger = LoggerFactory.getLogger(FileController.class);

  @GetMapping("/api/v1/file")
  public ResponseEntity<?> getFile(@RequestParam("fileId") Long fileId) {
    return ResponseEntity.status(HttpStatus.OK).body("Here is your file");
  }

  @PostMapping("/api/v1/file")
  public ResponseEntity<?> createFile(@RequestParam("file") MultipartFile file) {
    try {
      return ResponseEntity.status(HttpStatus.OK).body(("File Uploaded Successfully"));
    } catch (Exception e) {
      logger.error(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the file.");
    }
  }
}
