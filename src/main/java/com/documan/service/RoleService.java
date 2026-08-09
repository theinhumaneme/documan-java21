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
import com.documan.entity.RoleName;
import com.documan.entity.User;
import com.documan.exception.InvalidRequestException;
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

  /**
   * Privilege ordering comes from {@link RoleName}, not from the row's id.
   *
   * <p>It was the id, which held only as long as the seed script inserted the three roles in
   * ascending order of authority. Adding {@code maintainer} broke that: it ranks below a moderator
   * but an identity column can only append, so by id it outranked every role including admin, and
   * "promote to maintainer" would have been the strongest promotion available.
   *
   * <p>Refusing a sideways move — promoting to the role someone already holds — is deliberate. It
   * is always a mistake on the caller's part, and answering 200 would report a change that did not
   * happen.
   */
  private UserResponse changeRole(Integer userId, Integer roleId, boolean promoting) {
    User user = requireUser(userId);
    Role target = requireRole(roleId);
    int targetRank = RoleName.of(target.getName()).rank();
    int currentRank = RoleName.of(user.getRole().getName()).rank();

    if (promoting && targetRank <= currentRank) {
      throw new InvalidRequestException(
          "'%s' does not rank above the user's current role '%s'"
              .formatted(target.getName(), user.getRole().getName()));
    }
    if (!promoting && targetRank >= currentRank) {
      throw new InvalidRequestException(
          "'%s' does not rank below the user's current role '%s'"
              .formatted(target.getName(), user.getRole().getName()));
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
