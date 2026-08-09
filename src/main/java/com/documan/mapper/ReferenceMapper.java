// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.mapper;

import com.documan.dto.response.DepartmentResponse;
import com.documan.dto.response.RoleResponse;
import com.documan.dto.response.SemesterResponse;
import com.documan.dto.response.YearResponse;
import com.documan.entity.Department;
import com.documan.entity.Role;
import com.documan.entity.Semester;
import com.documan.entity.Year;
import org.mapstruct.Mapper;

/** Shared conversions for the small reference tables. */
@Mapper
public interface ReferenceMapper {

  DepartmentResponse toResponse(Department department);

  YearResponse toResponse(Year year);

  SemesterResponse toResponse(Semester semester);

  RoleResponse toResponse(Role role);
}
