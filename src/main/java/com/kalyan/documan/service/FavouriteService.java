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
public class FavouriteService {
  private final UserDao userDao;
  private final PostDao postDao;

  @Autowired
  public FavouriteService(UserDao userDao, PostDao postDao) {
    this.userDao = userDao;
    this.postDao = postDao;
  }

  public Optional<Post> favouritePost(Integer postId, Integer userId) {
    Optional<Post> post = postDao.findById(postId);
    Optional<User> user = userDao.findById(userId);
    if (post.isPresent() && user.isPresent()) {
      Post favouritedPost = post.get();
      List<User> favouriteUsers = favouritedPost.getFavouritedUsers();
      if (!favouriteUsers.contains(user.get())) {
        favouriteUsers.add(user.get());
        favouritedPost.setFavouritedUsers(favouriteUsers);
      }
      postDao.save(favouritedPost);
      return Optional.of(favouritedPost);
    }
    return Optional.empty();
  }

  public Optional<Post> removeFavouritePost(Integer postId, Integer userId) {
    Optional<Post> post = postDao.findById(postId);
    Optional<User> user = userDao.findById(userId);
    if (post.isPresent() && user.isPresent()) {
      Post favouritedPost = post.get();
      List<User> favouriteUsers = favouritedPost.getFavouritedUsers();
      if (favouriteUsers.contains(user.get())) {
        favouriteUsers.remove(user.get());
        favouritedPost.setFavouritedUsers(favouriteUsers);
      }
      postDao.save(favouritedPost);
      return Optional.of(favouritedPost);
    }
    return Optional.empty();
  }
}
