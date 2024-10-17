// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.dao;

import com.kalyan.documan.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterDao extends JpaRepository<Semester, Integer> {}
