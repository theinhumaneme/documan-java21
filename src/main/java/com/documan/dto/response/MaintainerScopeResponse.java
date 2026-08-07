// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * One grant.
 *
 * @param year null when the grant covers every year of the department
 * @param semester null when it covers every semester of the year
 */
public record MaintainerScopeResponse(
    Integer id,
    Integer userId,
    DepartmentResponse department,
    @Schema(nullable = true) YearResponse year,
    @Schema(nullable = true) SemesterResponse semester,
    OffsetDateTime dateCreated)
    implements Serializable {}
