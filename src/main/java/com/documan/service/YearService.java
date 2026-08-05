// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.config.CacheConfig;
import com.documan.dao.YearDao;
import com.documan.dto.response.YearResponse;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.ReferenceMapper;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class YearService {

  private final YearDao yearDao;
  private final ReferenceMapper referenceMapper;

  public YearService(YearDao yearDao, ReferenceMapper referenceMapper) {
    this.yearDao = yearDao;
    this.referenceMapper = referenceMapper;
  }

  @Cacheable(cacheNames = CacheConfig.YEARS, key = "#yearId")
  public YearResponse findById(Integer yearId) {
    return yearDao
        .findById(yearId)
        .map(referenceMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Year", yearId));
  }

  public List<YearResponse> findAll() {
    return yearDao.findAll().stream().map(referenceMapper::toResponse).toList();
  }
}
