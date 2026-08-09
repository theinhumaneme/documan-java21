// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank @Size(max = 40) String username,
    @NotBlank @Size(min = 8, max = 200) String password,
    @NotBlank @Size(max = 60) String firstName,
    @NotBlank @Size(max = 60) String lastName,
    @NotBlank @Email @Size(max = 180) String email,
    @NotNull Integer departmentId,
    @NotNull Integer yearId,
    @NotNull Integer semesterId,
    boolean acceptedTermsOfService) {}
