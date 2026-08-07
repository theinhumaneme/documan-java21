// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.dto.response.YearResponse;
import com.documan.service.YearService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/year")
public class YearController {

  private final YearService yearService;

  public YearController(YearService yearService) {
    this.yearService = yearService;
  }

  @GetMapping("/all")
  public List<YearResponse> getAllYears() {
    return yearService.findAll();
  }
}
