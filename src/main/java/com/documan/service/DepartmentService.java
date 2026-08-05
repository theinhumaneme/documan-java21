// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.config.CacheConfig;
import com.documan.dao.DepartmentDao;
import com.documan.dto.response.DepartmentResponse;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.ReferenceMapper;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

  private final DepartmentDao departmentDao;
  private final ReferenceMapper referenceMapper;

  public DepartmentService(DepartmentDao departmentDao, ReferenceMapper referenceMapper) {
    this.departmentDao = departmentDao;
    this.referenceMapper = referenceMapper;
  }

  @Cacheable(cacheNames = CacheConfig.DEPARTMENTS, key = "#departmentId")
  public DepartmentResponse findById(Integer departmentId) {
    return departmentDao
        .findById(departmentId)
        .map(referenceMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Department", departmentId));
  }

  public List<DepartmentResponse> findAll() {
    return departmentDao.findAll().stream().map(referenceMapper::toResponse).toList();
  }
}
