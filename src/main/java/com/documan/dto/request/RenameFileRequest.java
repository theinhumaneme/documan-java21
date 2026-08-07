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
 * Renames a file.
 *
 * <p>Only the display name changes. The object key in the bucket is derived once at upload from a
 * random prefix and is never rewritten, so a rename costs one column update and no object copy —
 * and an already-shared download URL keeps working.
 */
public record RenameFileRequest(@NotBlank @Size(max = 255) String name) {}
