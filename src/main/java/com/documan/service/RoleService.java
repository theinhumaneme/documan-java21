// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.config.CacheConfig;
import com.documan.dao.RoleDao;
import com.documan.dao.UserDao;
import com.documan.dto.response.RoleResponse;
import com.documan.dto.response.UserResponse;
import com.documan.entity.Role;
import com.documan.entity.User;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.ReferenceMapper;
import com.documan.mapper.UserMapper;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoleService {

  private final UserDao userDao;
  private final RoleDao roleDao;
  private final ReferenceMapper referenceMapper;
  private final UserMapper userMapper;

  public RoleService(
      UserDao userDao, RoleDao roleDao, ReferenceMapper referenceMapper, UserMapper userMapper) {
    this.userDao = userDao;
    this.roleDao = roleDao;
    this.referenceMapper = referenceMapper;
    this.userMapper = userMapper;
  }

  @Cacheable(cacheNames = CacheConfig.ROLES, key = "#roleId")
  public RoleResponse findById(Integer roleId) {
    return referenceMapper.toResponse(requireRole(roleId));
  }

  public List<RoleResponse> findAll() {
    return roleDao.findAll().stream().map(referenceMapper::toResponse).toList();
  }

  public RoleResponse findUserRole(Integer userId) {
    return referenceMapper.toResponse(requireUser(userId).getRole());
  }

  /**
   * Role changes now evict the cached user. Previously a promoted or demoted user kept serving the
   * stale role from {@code USER{id}} until the process restarted.
   */
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.USERS, key = "#userId")
  public UserResponse promote(Integer userId, Integer roleId) {
    return changeRole(userId, roleId, true);
  }

  @Transactional
  @CacheEvict(cacheNames = CacheConfig.USERS, key = "#userId")
  public UserResponse demote(Integer userId, Integer roleId) {
    return changeRole(userId, roleId, false);
  }

  /** Privilege ordering is by numeric role id, matching the seeded regular/moderator/admin rows. */
  private UserResponse changeRole(Integer userId, Integer roleId, boolean promoting) {
    User user = requireUser(userId);
    Role target = requireRole(roleId);
    int current = user.getRole().getId();

    if (promoting && roleId <= current) {
      throw new IllegalArgumentException(
          "Role %d does not rank above the user's current role %d".formatted(roleId, current));
    }
    if (!promoting && roleId >= current) {
      throw new IllegalArgumentException(
          "Role %d does not rank below the user's current role %d".formatted(roleId, current));
    }

    user.setRole(target);
    return userMapper.toResponse(userDao.save(user));
  }

  private Role requireRole(Integer roleId) {
    return roleDao
        .findById(roleId)
        .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
  }

  private User requireUser(Integer userId) {
    return userDao
        .findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", userId));
  }
}
