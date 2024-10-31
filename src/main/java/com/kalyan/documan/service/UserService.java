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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserDao userDao;
  private final SubjectDao subjectDao;
  private final DepartmentDao departmentDao;
  private final YearDao yearDao;
  private final SemesterDao semesterDao;
  private final RoleDao roleDao;

  @Autowired
  public UserService(
      UserDao userDao,
      SubjectDao subjectDao,
      DepartmentDao departmentDao,
      YearDao yearDao,
      SemesterDao semesterDao,
      RoleDao roleDao) {
    this.userDao = userDao;
    this.subjectDao = subjectDao;
    this.departmentDao = departmentDao;
    this.yearDao = yearDao;
    this.semesterDao = semesterDao;
    this.roleDao = roleDao;
  }

  public Optional<User> findById(Integer userId) {
    return userDao.findById(userId);
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
      User user, Integer departmentId, Integer yearId, Integer semesterId, Integer roleId) {
    Optional<Department> department = departmentDao.findById(departmentId);
    Optional<Year> year = yearDao.findById(yearId);
    Optional<Semester> semester = semesterDao.findById(semesterId);
    Optional<Role> role = roleDao.findById(roleId);
    if (department.isEmpty() || year.isEmpty() || semester.isEmpty() || role.isEmpty()) {
      return Optional.empty();
    } else if (user.getId() != null) {
      return Optional.empty();
    } else {
      User newUser = new User();
      newUser.setFirstName(user.getFirstName());
      newUser.setUsername(user.getUsername());
      newUser.setLastName(user.getLastName());
      newUser.setEmail(user.getEmail());
      newUser.setPassword(user.getPassword());
      newUser.setDepartment(department.get());
      newUser.setYear(year.get());
      newUser.setSemester(semester.get());
      newUser.setRole(role.get());
      User updatedEntity = userDao.save(newUser);
      return Optional.of(updatedEntity);
    }
  }

  public Optional<User> updateUser(
      User user,
      Integer userId,
      Integer departmentId,
      Integer yearId,
      Integer semesterId,
      Integer roleId) {
    Optional<User> oldUserData = userDao.findById(userId);
    Optional<Department> department = departmentDao.findById(departmentId);
    Optional<Year> year = yearDao.findById(yearId);
    Optional<Semester> semester = semesterDao.findById(semesterId);
    Optional<Role> role = roleDao.findById(roleId);
    if (department.isEmpty()
        || year.isEmpty()
        || semester.isEmpty()
        || oldUserData.isEmpty()
        || role.isEmpty()) {
      return Optional.empty();
    } else {
      User updatedUser = oldUserData.get();
      updatedUser.setFirstName(user.getFirstName());
      updatedUser.setLastName(user.getLastName());
      updatedUser.setEmail(user.getEmail());
      updatedUser.setPassword(user.getPassword());
      updatedUser.setDepartment(department.get());
      updatedUser.setYear(year.get());
      updatedUser.setSemester(semester.get());
      updatedUser.setRole(role.get());
      User updatedEntity = userDao.save(updatedUser);
      return Optional.of(updatedEntity);
    }
  }
}
