// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.dto.response.SemesterResponse;
import com.documan.service.SemesterService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/semester")
public class SemesterController {

  private final SemesterService semesterService;

  public SemesterController(SemesterService semesterService) {
    this.semesterService = semesterService;
  }

  @GetMapping
  public SemesterResponse getSemester(@RequestParam("semesterId") Integer semesterId) {
    return semesterService.findById(semesterId);
  }

  @GetMapping("/all")
  public List<SemesterResponse> getAllSemesters() {
    return semesterService.findAll();
  }
}
