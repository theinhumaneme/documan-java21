// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dao;

import com.documan.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleDao extends JpaRepository<Role, Integer> {

  /**
   * Roles are looked up by the name that means something rather than by a seeded id. See {@code
   * RoleName} for why nothing may depend on the numbering.
   */
  Optional<Role> findByName(String name);
}
