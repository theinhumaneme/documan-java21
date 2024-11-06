// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.dao.SemesterDao;
import com.documan.entity.Semester;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SemesterService {
  private static final Logger log = LoggerFactory.getLogger(SemesterService.class);
  private final SemesterDao semesterDao;

  @Autowired
  public SemesterService(SemesterDao semesterDao) {
    this.semesterDao = semesterDao;
  }

  public Optional<Semester> getSemesterById(Integer semesterId) {
    return semesterDao.findById(semesterId);
  }

  public Optional<List<Semester>> getAllSemesters() {
    return Optional.of(semesterDao.findAll());
  }
}
