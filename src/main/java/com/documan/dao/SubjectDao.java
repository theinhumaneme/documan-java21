// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dao;

import com.documan.entity.Subject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubjectDao extends JpaRepository<Subject, Integer> {

  @Query(
      value =
          "select * from subject s where s.department_id = :departmentId and s.year_id = :yearId and s.semester_id = :semesterId",
      nativeQuery = true)
  public List<Subject> getSubjects(
      @Param("departmentId") Integer departmentId,
      @Param("yearId") Integer yearId,
      @Param("semesterId") Integer semesterId);
}
