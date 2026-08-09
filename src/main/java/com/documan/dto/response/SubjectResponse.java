// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.response;

import java.io.Serializable;

public record SubjectResponse(
    Integer id,
    String name,
    String code,
    boolean lab,
    boolean theory,
    DepartmentResponse department,
    YearResponse year,
    SemesterResponse semester)
    implements Serializable {}
