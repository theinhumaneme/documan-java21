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
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Grants of part of the library to a maintainer.
 *
 * <p>Granting and revoking are an administrator's job, and reading a maintainer's own grants is how
 * the client decides which departments, years and semesters to offer them.
 *
 * <p>Both are now enforced. The javadoc here used to say the opposite — that the application had no
 * authentication and these endpoints were open like every other one — which was true when
 * {@link MaintainerScopeService#mayEdit} was written and stopped being true when the service became
 * a resource server. A grant is the right to change part of the library, so handing one out is
 * exactly as consequential as a promotion, and is gated the same way.
 */
@RestController
@RequestMapping("/api/v1/maintainer-scope")
public class MaintainerScopeController {

  private final MaintainerScopeService scopeService;

  public MaintainerScopeController(MaintainerScopeService scopeService) {
    this.scopeService = scopeService;
  }

  /**
   * A plain array: a maintainer holds a handful of grants, not a page of them.
   *
   * <p>Your own, or anyone's if you moderate. A maintainer needs this to know which part of the
   * library to offer them; the console needs it for everybody. What neither needs is one maintainer
   * reading another's grants, which is a map of who can change what.
   */
  @GetMapping
  @PreAuthorize("@permissions.isSelfOrModerator(#userId)")
  public List<MaintainerScopeResponse> getScopes(@RequestParam("userId") Integer userId) {
    return scopeService.findByUser(userId);
  }

  /** Whether a maintainer may change what is filed at one address. */
  @GetMapping("/may-edit")
  @PreAuthorize("@permissions.isSelfOrModerator(#userId)")
  public boolean mayEdit(
      @RequestParam("userId") Integer userId,
      @RequestParam("departmentId") Integer departmentId,
      @RequestParam(value = "yearId", required = false) Integer yearId,
      @RequestParam(value = "semesterId", required = false) Integer semesterId) {
    return scopeService.mayEdit(userId, departmentId, yearId, semesterId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@permissions.isAdmin()")
  public MaintainerScopeResponse grant(@Valid @RequestBody GrantScopeRequest request) {
    return scopeService.grant(request);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@permissions.isAdmin()")
  public void revoke(@RequestParam("scopeId") Integer scopeId) {
    scopeService.revoke(scopeId);
  }
}
