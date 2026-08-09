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
 * Renames a folder.
 *
 * <p>The slug is not changed. It is what a bookmarked URL points at, so renaming "Reference Books"
 * to "References" should not break a link that was already shared — and the slug is only ever a
 * stable handle, never something a reader sees.
 */
public record UpdateFolderRequest(@NotBlank @Size(max = 100) String name) {}
