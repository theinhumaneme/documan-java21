// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.dao;

import com.kalyan.documan.entity.Post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostDao extends JpaRepository<Post, Integer> {
  @Query(value = "SELECT * FROM post where user_id =: userId", nativeQuery = true)
  public List<Post> getPostsByUserId(int userId);
}
