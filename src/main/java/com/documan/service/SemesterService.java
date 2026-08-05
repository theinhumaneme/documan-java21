// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.config.CacheConfig;
import com.documan.dao.SemesterDao;
import com.documan.dto.response.SemesterResponse;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.ReferenceMapper;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SemesterService {

  private final SemesterDao semesterDao;
  private final ReferenceMapper referenceMapper;

  public SemesterService(SemesterDao semesterDao, ReferenceMapper referenceMapper) {
    this.semesterDao = semesterDao;
    this.referenceMapper = referenceMapper;
  }

  @Cacheable(cacheNames = CacheConfig.SEMESTERS, key = "#semesterId")
  public SemesterResponse findById(Integer semesterId) {
    return semesterDao
        .findById(semesterId)
        .map(referenceMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Semester", semesterId));
  }

  public List<SemesterResponse> findAll() {
    return semesterDao.findAll().stream().map(referenceMapper::toResponse).toList();
  }
}
