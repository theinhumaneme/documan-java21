// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.dto.request.GrantScopeRequest;
import com.documan.dto.response.MaintainerScopeResponse;
import com.documan.service.MaintainerScopeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Grants of part of the library to a maintainer.
 *
 * <p>Granting and revoking are an administrator's job, and reading a maintainer's own grants is how
 * the client decides which departments, years and semesters to offer them. None of that is enforced
 * here: the application has no authentication, so these endpoints are open like every other one.
 * The rule is written and the rows are real; the principal to test them against is what is missing.
 */
@RestController
@RequestMapping("/api/v1/maintainer-scope")
public class MaintainerScopeController {

  private final MaintainerScopeService scopeService;

  public MaintainerScopeController(MaintainerScopeService scopeService) {
    this.scopeService = scopeService;
  }

  /** A plain array: a maintainer holds a handful of grants, not a page of them. */
  @GetMapping
  public List<MaintainerScopeResponse> getScopes(@RequestParam("userId") Integer userId) {
    return scopeService.findByUser(userId);
  }

  /** Whether a maintainer may change what is filed at one address. */
  @GetMapping("/may-edit")
  public boolean mayEdit(
      @RequestParam("userId") Integer userId,
      @RequestParam("departmentId") Integer departmentId,
      @RequestParam(value = "yearId", required = false) Integer yearId,
      @RequestParam(value = "semesterId", required = false) Integer semesterId) {
    return scopeService.mayEdit(userId, departmentId, yearId, semesterId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MaintainerScopeResponse grant(@Valid @RequestBody GrantScopeRequest request) {
    return scopeService.grant(request);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(@RequestParam("scopeId") Integer scopeId) {
    scopeService.revoke(scopeId);
  }
}
