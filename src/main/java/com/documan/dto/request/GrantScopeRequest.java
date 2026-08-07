// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Grants a maintainer part of the library.
 *
 * <p>Omit {@code yearId} to grant the whole department, and {@code semesterId} to grant a whole
 * year. A semester without a year is rejected: it would describe "this semester of every year",
 * which is not a grant anyone means to make.
 */
public record GrantScopeRequest(
    @NotNull Integer userId,
    @NotNull Integer departmentId,
    Integer yearId,
    Integer semesterId) {}
