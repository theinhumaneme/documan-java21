// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.SubjectDao;
import com.kalyan.documan.dao.UserDao;
import com.kalyan.documan.entity.*;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserService {

  private final UserDao userDao;
  private final SubjectDao subjectDao;

  @Autowired
  public UserService(UserDao userDao, SubjectDao subjectDao) {
    this.userDao = userDao;
    this.subjectDao = subjectDao;
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
            subjectDao.getUserSubjects(
                value.getDepartment().getId(),
                value.getYear().getId(),
                value.getSemester().getId()));
  }
}
