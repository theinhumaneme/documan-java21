// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.YearDao;
import com.kalyan.documan.entity.Year;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class YearService {
  private static final Logger log = LoggerFactory.getLogger(YearService.class);
  private final YearDao yearDao;

  @Autowired
  public YearService(YearDao yearDao) {
    this.yearDao = yearDao;
  }

  public Optional<Year> getYearById(Integer yearId) {
    return yearDao.findById(yearId);
  }

  public Optional<List<Year>> getAllYears() {
    return Optional.of(yearDao.findAll());
  }
}
