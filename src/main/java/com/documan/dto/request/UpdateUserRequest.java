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

/**
 * {@code password} is optional: a null value leaves the stored credential untouched. The previous
 * implementation overwrote the password on every profile update.
 */
public record UpdateUserRequest(
    @NotBlank @Size(max = 40) String username,
    @Size(min = 8, max = 200) String password,
    @NotBlank @Size(max = 60) String firstName,
    @NotBlank @Size(max = 60) String lastName,
    @NotBlank @Email @Size(max = 180) String email,
    @NotNull Integer departmentId,
    @NotNull Integer yearId,
    @NotNull Integer semesterId,
    Boolean acceptedTermsOfService,
    Boolean canPost,
    Boolean canComment) {}
