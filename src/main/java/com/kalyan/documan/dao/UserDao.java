/* Copyright (C)2024 Mudumby Kalyan / @theinhumaneme  */
package com.kalyan.documan.dao;

import com.kalyan.documan.entity.Subject;
import com.kalyan.documan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDao extends JpaRepository<User, Integer> {

  /*
   TODO: `create a function to fetch all the user's subjects

  */
  public Subject getUserSubjects(
      Integer userId, Integer departmentId, Integer yearId, Integer semesterId) {
    /* TODO */
  }
}
