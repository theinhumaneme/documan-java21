// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.SubjectDao;
import com.kalyan.documan.entity.Subject;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SubjectService {

  private final SubjectDao subjectDao;

  @Autowired
  public SubjectService(SubjectDao subjectDao) {
    this.subjectDao = subjectDao;
  }

  public Optional<List<Subject>> getAllSubjects() {
    return Optional.of(subjectDao.findAll());
  }

  public Optional<Subject> getSubjectById(Integer id) {
    return subjectDao.findById(id);
  }

  public Optional<List<Subject>> getSubjects(
      Integer departmentId, Integer yearId, Integer semesterId) {
    return Optional.of(subjectDao.getSubjects(departmentId, yearId, semesterId));
  }

  public Optional<List<Subject>> getUserSubjects(
      Integer departmentId, Integer yearId, Integer semesterId) {
    return Optional.of(subjectDao.getSubjects(departmentId, yearId, semesterId));
  }
}
