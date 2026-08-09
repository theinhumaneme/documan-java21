// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.dto.response.RoleResponse;
import com.documan.dto.response.UserResponse;
import com.documan.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/role")
public class RoleController {

  private final RoleService roleService;

  public RoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  @GetMapping
  public RoleResponse getRole(@RequestParam("roleId") Integer roleId) {
    return roleService.findById(roleId);
  }

  @GetMapping("/all")
  public List<RoleResponse> getAllRoles() {
    return roleService.findAll();
  }

  @GetMapping("/user")
  public RoleResponse getUserRole(@RequestParam("userId") Integer userId) {
    return roleService.findUserRole(userId);
  }

  /**
   * Administrators only, and this is the endpoint that most needed saying so.
   *
   * <p>{@code changeRole} validates the direction of the move and nothing about the caller, so
   * before this annotation any holder of a valid token could promote themselves to {@code admin}
   * with two integers — and {@code GET /role/all} above is a public read that hands out the second
   * one. There is deliberately no exception for promoting yourself downward: demotion is still an
   * administrator's act, and an administrator who wants to step down can be stepped down.
   */
  @PutMapping("/promote")
  @PreAuthorize("@permissions.isAdmin()")
  public UserResponse promoteUser(
      @RequestParam("userId") Integer userId, @RequestParam("roleId") Integer roleId) {
    return roleService.promote(userId, roleId);
  }

  @PutMapping("/demote")
  @PreAuthorize("@permissions.isAdmin()")
  public UserResponse demoteUser(
      @RequestParam("userId") Integer userId, @RequestParam("roleId") Integer roleId) {
    return roleService.demote(userId, roleId);
  }
}
