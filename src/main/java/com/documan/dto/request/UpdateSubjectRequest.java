// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSubjectRequest(
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Size(max = 20) String code,
    boolean lab,
    boolean theory,
    @NotNull Integer departmentId,
    @NotNull Integer yearId,
    @NotNull Integer semesterId) {}
