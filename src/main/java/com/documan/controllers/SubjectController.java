// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.dto.request.CreateSubjectRequest;
import com.documan.dto.request.UpdateSubjectRequest;
import com.documan.dto.response.PageResponse;
import com.documan.dto.response.SubjectResponse;
import com.documan.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subject")
public class SubjectController {

  private final SubjectService subjectService;

  public SubjectController(SubjectService subjectService) {
    this.subjectService = subjectService;
  }

  @GetMapping
  public SubjectResponse getSubject(@RequestParam("subjectId") Integer subjectId) {
    return subjectService.findById(subjectId);
  }

  @GetMapping("/all")
  public PageResponse<SubjectResponse> getAllSubjects(
      @PageableDefault(size = 50, sort = "name") Pageable pageable) {
    return subjectService.findAll(pageable);
  }

  @GetMapping("/semester")
  public PageResponse<SubjectResponse> getSubjects(
      @RequestParam("departmentId") Integer departmentId,
      @RequestParam("yearId") Integer yearId,
      @RequestParam("semesterId") Integer semesterId,
      @PageableDefault(size = 50, sort = "name") Pageable pageable) {
    return subjectService.findBy(departmentId, yearId, semesterId, pageable);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SubjectResponse createSubject(@Valid @RequestBody CreateSubjectRequest request) {
    return subjectService.create(request);
  }

  @PutMapping
  public SubjectResponse updateSubject(
      @Valid @RequestBody UpdateSubjectRequest request,
      @RequestParam("subjectId") Integer subjectId) {
    return subjectService.update(subjectId, request);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSubject(@RequestParam("subjectId") Integer subjectId) {
    subjectService.delete(subjectId);
  }
}
