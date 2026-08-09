// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A folder a maintainer adds themselves — "Previous Papers", "Reference Books".
 *
 * <p>Only a name. The slug is derived from it by the service, so a caller cannot mint a slug that
 * collides with a default folder's and shadow it.
 */
public record CreateFolderRequest(@NotBlank @Size(max = 100) String name) {}
