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
import org.springframework.security.access.prepost.PreAuthorize;
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

  /**
   * Sorted by name, then by id, and the second one is not decoration.
   *
   * <p>{@code name} is not unique — "Electromagnetics and Transmission Lines" is a subject in both
   * Electronics and Mechanical — so ordering by it alone is a partial order. Rows that compare equal
   * may come back in a different arrangement for each {@code LIMIT/OFFSET} window, which lets one
   * sit after the boundary on one page and before it on the next, so a client walking the pages
   * never sees it. That is not theoretical: paging this endpoint during the bulk import skipped both
   * of those subjects, and the importer created a second copy of each. A unique tiebreaker makes the
   * order total and the paging stable.
   */
  @GetMapping("/all")
  public PageResponse<SubjectResponse> getAllSubjects(
      @PageableDefault(size = 50, sort = {"name", "id"}) Pageable pageable) {
    return subjectService.findAll(pageable);
  }

  @GetMapping("/semester")
  public PageResponse<SubjectResponse> getSubjects(
      @RequestParam("departmentId") Integer departmentId,
      @RequestParam("yearId") Integer yearId,
      @RequestParam("semesterId") Integer semesterId,
      @PageableDefault(size = 50, sort = {"name", "id"}) Pageable pageable) {
    return subjectService.findBy(departmentId, yearId, semesterId, pageable);
  }

  /** The address is in the body, so the grant is checked against that rather than an existing row. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(
      "@permissions.mayEditAddress(#request.departmentId(), #request.yearId(),"
          + " #request.semesterId())")
  public SubjectResponse createSubject(@Valid @RequestBody CreateSubjectRequest request) {
    return subjectService.create(request);
  }

  /**
   * Both addresses, because an update may move the subject.
   *
   * <p>{@code UpdateSubjectRequest} carries a department, year and semester, so this can file a
   * subject somewhere new. Checking only where it currently sits would let a maintainer push one out
   * of their scope into a department they have no grant over; checking only the destination would
   * let them pull one in. The same reasoning as moving files.
   */
  @PutMapping
  @PreAuthorize(
      "@permissions.mayEditSubject(#subjectId) and @permissions.mayEditAddress("
          + "#request.departmentId(), #request.yearId(), #request.semesterId())")
  public SubjectResponse updateSubject(
      @Valid @RequestBody UpdateSubjectRequest request,
      @RequestParam("subjectId") Integer subjectId) {
    return subjectService.update(subjectId, request);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@permissions.mayEditSubject(#subjectId)")
  public void deleteSubject(@RequestParam("subjectId") Integer subjectId) {
    subjectService.delete(subjectId);
  }
}
