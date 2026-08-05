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

  @PutMapping("/promote")
  public UserResponse promoteUser(
      @RequestParam("userId") Integer userId, @RequestParam("roleId") Integer roleId) {
    return roleService.promote(userId, roleId);
  }

  @PutMapping("/demote")
  public UserResponse demoteUser(
      @RequestParam("userId") Integer userId, @RequestParam("roleId") Integer roleId) {
    return roleService.demote(userId, roleId);
  }
}
