// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dao;

import com.documan.entity.Subject;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectDao extends JpaRepository<Subject, Integer> {

  /** Replaces the previous native query; backed by idx_subject_department_year_semester. */
  Page<Subject> findByDepartmentIdAndYearIdAndSemesterId(
      Integer departmentId, Integer yearId, Integer semesterId, Pageable pageable);

  /** Batch load for indexing, joining the academic references in one statement. */
  @EntityGraph(attributePaths = {"department", "year", "semester"})
  List<Subject> findForIndexingByIdIn(Collection<Integer> ids);
}
