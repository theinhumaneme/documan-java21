// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.controllers;

import com.kalyan.documan.entity.Subject;
import com.kalyan.documan.service.SubjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subject")
public class SubjectController {
  private static final Logger log = LoggerFactory.getLogger(SubjectController.class);
  private final SubjectService subjectService;

  @Autowired
  public SubjectController(SubjectService subjectService) {
    this.subjectService = subjectService;
  }

  @GetMapping
  public ResponseEntity<?> getSubject(@RequestParam("subjectId") Integer subjectId) {
    try {
      return subjectService
          .getSubjectById(subjectId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @GetMapping("/all")
  public ResponseEntity<?> getAllSubjects() {
    try {
      return subjectService
          .getAllSubjects()
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @GetMapping("/semester")
  public ResponseEntity<?> getSubjects(
      @RequestParam("departmentId") Integer departmentId,
      @RequestParam("yearId") Integer yearId,
      @RequestParam("semesterId") Integer semesterId) {
    try {
      return subjectService
          .getSubjects(departmentId, yearId, semesterId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing your request");
    }
  }

  @PostMapping
  public ResponseEntity<?> createSubject(
      @RequestBody Subject subject,
      @RequestParam("departmentId") Integer departmentId,
      @RequestParam("yearId") Integer yearId,
      @RequestParam("semesterId") Integer semesterId) {
    try {
      return subjectService
          .createSubject(subject, departmentId, yearId, semesterId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the subject.");
    }
  }

  @PutMapping
  public ResponseEntity<?> updateSubject(
      @RequestBody Subject subject,
      @RequestParam("subjectId") Integer subjectId,
      @RequestParam("departmentId") Integer departmentId,
      @RequestParam("yearId") Integer yearId,
      @RequestParam("semesterId") Integer semesterId) {
    try {
      return subjectService
          .updateSubject(subject, subjectId, departmentId, yearId, semesterId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the subject.");
    }
  }

  @DeleteMapping
  public ResponseEntity<?> deleteSubject(@RequestParam("subjectId") Integer subjectId) {
    try {
      return subjectService
          .deleteSubject(subjectId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    } catch (Exception e) {
      log.error(e.toString());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while processing the subject.");
    }
  }
}
