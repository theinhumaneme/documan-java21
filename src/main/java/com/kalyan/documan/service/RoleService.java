// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.RoleDao;
import com.kalyan.documan.dao.UserDao;
import com.kalyan.documan.entity.Role;
import com.kalyan.documan.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

  private final UserDao userDao;
  private final RoleDao roleDao;

  @Autowired
  public RoleService(UserDao userDao, RoleDao roleDao) {
    this.userDao = userDao;
    this.roleDao = roleDao;
  }

  public Optional<Role> getRoleById(Integer id) {
    return roleDao.findById(id);
  }

  public Optional<List<Role>> getAllRoles() {
    return Optional.of(roleDao.findAll());
  }

  public Optional<Role> getUserRole(Integer userId) {
    Optional<User> user = userDao.findById(userId);
    return user.map(User::getRole);
  }

  public Optional<User> promoteUser(Integer userId, Integer roleId) {
    Optional<Role> role = roleDao.findById(roleId);
    Optional<User> user = userDao.findById(userId);
    if (role.isPresent() && user.isPresent() && roleId > user.get().getRole().getId()) {
      User promotedUser = user.get();
      promotedUser.setRole(role.get());
      return Optional.of(userDao.save(promotedUser));
    } else {
      return Optional.empty();
    }
  }

  public Optional<User> demoteUser(Integer userId, Integer roleId) {
    Optional<Role> role = roleDao.findById(roleId);
    Optional<User> user = userDao.findById(userId);
    if (role.isPresent() && user.isPresent() && roleId < user.get().getRole().getId()) {
      User demotedUser = user.get();
      demotedUser.setRole(role.get());
      return Optional.of(userDao.save(demotedUser));
    } else {
      return Optional.empty();
    }
  }
}
