// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.DepartmentDao;
import com.kalyan.documan.entity.Department;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DepartmentService {
  private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);
  private final DepartmentDao departmentDao;

  @Autowired
  public DepartmentService(DepartmentDao departmentDao) {
    this.departmentDao = departmentDao;
  }

  public Optional<Department> getDepartmentById(Integer departmentId) {
    return departmentDao.findById(departmentId);
  }

  public Optional<List<Department>> getAllDepartments() {
    return Optional.of(departmentDao.findAll());
  }
}
