// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dao;

import com.documan.entity.MaintainerScope;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintainerScopeDao extends JpaRepository<MaintainerScope, Integer> {

  /**
   * Every grant held by one maintainer. The address rows are eager on the entity because a grant is
   * meaningless without them — the graph keeps a list of grants from costing three selects each.
   */
  @EntityGraph(attributePaths = {"department", "year", "semester"})
  List<MaintainerScope> findByUserId(Integer userId);

  void deleteByUserId(Integer userId);
}
