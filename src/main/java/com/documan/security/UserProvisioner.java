// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.security;

import com.documan.dao.UserDao;
import com.documan.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The insert half of provisioning, in a transaction of its own.
 *
 * <p>It is a separate bean for one reason: {@code REQUIRES_NEW} is applied by the proxy, and a call
 * from one method of {@link CurrentUser} to another would not pass through it. Written inside that
 * class this annotation would be silently ignored, which is a worse failure than not having it.
 *
 * <p>Why a separate transaction at all: two requests can reach provisioning for the same person at
 * the same moment, and one of them will lose on a unique index. That failure has to be recoverable
 * — the loser should read the winner's row and carry on — but a {@code
 * DataIntegrityViolationException} marks its transaction rollback-only, so catching it in the
 * transaction that also has to do the reading leaves nothing usable. Isolating the insert means the
 * caller's transaction survives the loss and can simply look again.
 */
@Component
public class UserProvisioner {

  private final UserDao userDao;

  public UserProvisioner(UserDao userDao) {
    this.userDao = userDao;
  }

  /**
   * Insert, committing on its own.
   *
   * <p>{@code saveAndFlush} rather than {@code save}: the statement has to reach the database
   * inside this call so a constraint violation is thrown here, where the caller can catch it. With
   * {@code save} the insert is deferred to commit and the exception surfaces somewhere else
   * entirely.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public User insert(User user) {
    return userDao.saveAndFlush(user);
  }
}
