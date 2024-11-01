// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.*;
import com.kalyan.documan.entity.*;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);
  private final UserDao userDao;
  private final SubjectDao subjectDao;
  private final DepartmentDao departmentDao;
  private final YearDao yearDao;
  private final SemesterDao semesterDao;
  private final RedisCacheService redisCacheService;
  private final RoleDao roleDao;

  @Autowired
  public UserService(
      UserDao userDao,
      SubjectDao subjectDao,
      DepartmentDao departmentDao,
      YearDao yearDao,
      SemesterDao semesterDao,
      RedisCacheService redisCacheService,
      RoleDao roleDao) {
    this.userDao = userDao;
    this.subjectDao = subjectDao;
    this.departmentDao = departmentDao;
    this.yearDao = yearDao;
    this.semesterDao = semesterDao;
    this.redisCacheService = redisCacheService;
    this.roleDao = roleDao;
  }

  public Optional<User> findById(Integer userId) {
    String userKey = String.format("USER%s", userId);
    Optional<User> cachedEntity = redisCacheService.getValue(userKey, User.class);
    if (cachedEntity.isEmpty()) {
      log.error("User {} not found in cache", userId);
      Optional<User> user = userDao.findById(userId);
      if (user.isPresent()) {
        Optional<User> cachedUser = redisCacheService.setValue(userKey, user.get());
        if (cachedUser.isEmpty()) {
          log.error("Failed to cache User {}", userId);
        } else {
          log.info("cached User {}", userId);
        }
        return user; // return user from db cache if exists
      } else {
        return Optional.empty();
      }
    } else {
      log.info("User {} found in cache", userId);
    }
    return cachedEntity;
  }

  public Optional<User> findByUsername(String username) {
    return Optional.ofNullable(userDao.findByUsername(username));
  }

  public Optional<User> findByEmail(String email) {
    return Optional.ofNullable(userDao.findByEmail(email));
  }

  public Optional<List<Post>> getUserPosts(Integer userId) {
    Optional<User> user = userDao.findById(userId);
    return user.map(User::getPosts);
  }

  public Optional<List<Comment>> getUserComments(Integer userId) {
    Optional<User> user = userDao.findById(userId);
    return user.map(User::getComments);
  }

  public Optional<List<File>> getUserFavouriteFiles(Integer userId) {
    Optional<User> user = userDao.findById(userId);
    return user.map(User::getFavouriteFiles);
  }

  public Optional<List<Post>> getUserFavouritePosts(Integer userId) {
    Optional<User> user = userDao.findById(userId);
    return user.map(User::getFavoritePosts);
  }

  public Optional<List<Post>> getUserUpvotedPosts(Integer userId) {
    Optional<User> user = userDao.findById(userId);
    return user.map(User::getUpvotedPosts);
  }

  public Optional<List<Comment>> getUserUpvotedComments(Integer userId) {
    Optional<User> user = userDao.findById(userId);
    return user.map(User::getUpvotedComments);
  }

  public Optional<List<Post>> getUserDownvotedPosts(Integer userId) {
    Optional<User> user = userDao.findById(userId);
    return user.map(User::getDownvotedPosts);
  }

  public Optional<List<Comment>> getUserDownvotedComments(Integer userId) {
    Optional<User> user = userDao.findById(userId);
    return user.map(User::getDownvotedComments);
  }

  public Optional<List<Subject>> getUserSubjects(Integer userId) {
    Optional<User> user = userDao.findById(userId);
    return user.map(
        value ->
            subjectDao.getSubjects(
                value.getDepartment().getId(),
                value.getYear().getId(),
                value.getSemester().getId()));
  }

  public Optional<User> createUser(
      User user, Integer departmentId, Integer yearId, Integer semesterId) {
    Optional<Department> department = departmentDao.findById(departmentId);
    Optional<Year> year = yearDao.findById(yearId);
    Optional<Semester> semester = semesterDao.findById(semesterId);
    Optional<Role> role = roleDao.findById(1); // always set new user to regular role
    if (department.isEmpty() || year.isEmpty() || semester.isEmpty() || role.isEmpty()) {
      return Optional.empty();
    } else if (user.getId() != null) {
      return Optional.empty();
    } else {
      User newUser = new User();
      newUser.setUsername(user.getUsername());
      newUser.setFirstName(user.getFirstName());
      newUser.setLastName(user.getLastName());
      newUser.setEmail(user.getEmail());
      newUser.setPassword(user.getPassword());
      newUser.setDepartment(department.get());
      newUser.setYear(year.get());
      newUser.setSemester(semester.get());
      newUser.setRole(role.get());
      User newEntity = userDao.save(newUser);
      String userKey = String.format("USER%s", newEntity.getId());
      Optional<User> cachedEntity = redisCacheService.setValue(userKey, newEntity);
      if (cachedEntity.isEmpty()) {
        log.error("Failed to cache User {}", newEntity.getId());
      } else {
        log.info("User {} cached", newEntity.getId());
      }
      return Optional.of(newEntity);
    }
  }

  public Optional<User> updateUser(
      User user, Integer userId, Integer departmentId, Integer yearId, Integer semesterId) {
    Optional<User> oldUserData = userDao.findById(userId);
    Optional<Department> department = departmentDao.findById(departmentId);
    Optional<Year> year = yearDao.findById(yearId);
    Optional<Semester> semester = semesterDao.findById(semesterId);
    if (department.isEmpty() || year.isEmpty() || semester.isEmpty() || oldUserData.isEmpty()) {
      return Optional.empty();
    } else {
      User updatedUser = oldUserData.get();
      updatedUser.setUsername(user.getUsername());
      updatedUser.setFirstName(user.getFirstName());
      updatedUser.setLastName(user.getLastName());
      updatedUser.setEmail(user.getEmail());
      updatedUser.setPassword(user.getPassword());
      updatedUser.setDepartment(department.get());
      updatedUser.setYear(year.get());
      updatedUser.setSemester(semester.get());
      User updatedEntity = userDao.save(updatedUser);
      String userKey = String.format("USER%s", updatedEntity.getId());
      Optional<User> cachedEntity = redisCacheService.updateValue(userKey, updatedEntity);
      if (cachedEntity.isEmpty()) {
        log.error("Failed to update User {} in cache", updatedEntity.getId());
      } else {
        log.info("User {} updated in cache", updatedEntity.getId());
      }
      return Optional.of(updatedEntity);
    }
  }
}
