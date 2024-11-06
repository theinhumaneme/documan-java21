// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.service.CloudflareR2Service;
import com.documan.service.SubjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/v1/file")
@RestController
public class FileController {
  private static final Logger log = LoggerFactory.getLogger(FileController.class);
  private final CloudflareR2Service cloudflareR2Service;
  private final SubjectService subjectService;

  public FileController(CloudflareR2Service cloudflareR2Service, SubjectService subjectService) {
    this.cloudflareR2Service = cloudflareR2Service;
    this.subjectService = subjectService;
  }

  @GetMapping("/subject")
  public ResponseEntity<?> getSubjectFiles(@RequestParam("subjectId") Integer subjectId) {
    try {
      return subjectService
          .getSubjectFiles(subjectId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @PostMapping
  public ResponseEntity<?> createFile(
      @RequestParam("file") MultipartFile file, @RequestParam("subjectId") Integer subjectId) {
    try {
      return cloudflareR2Service
          .uploadFile(file, subjectId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the file.");
    }
  }

  @DeleteMapping
  public ResponseEntity<?> deleteFile(@RequestParam("objectUID") String objectUID) {
    try {
      return cloudflareR2Service
          .deleteFile(objectUID)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the file.");
    }
  }
}
