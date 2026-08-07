// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
    @NotBlank @Size(max = 150) String title,
    @NotBlank @Size(max = 400) String description,
    @NotBlank String content,
    boolean commentsClosed,
    boolean announcement) {}
