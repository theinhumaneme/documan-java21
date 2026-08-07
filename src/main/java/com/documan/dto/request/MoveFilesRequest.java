// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Moves files into a folder.
 *
 * <p>A list rather than one file per request, because the interface offers multi-select and twenty
 * round trips for a twenty-file move is the wrong shape — and because a partial move is a worse
 * outcome than a failed one, so the whole batch shares a transaction.
 *
 * <p>The destination is always a folder, and the subject comes from it. That makes moving a file to
 * another subject the same operation as filing it within one, and there is no second kind of
 * destination to validate: every file is in a folder, so every move is folder to folder.
 */
public record MoveFilesRequest(@NotEmpty List<Integer> fileIds, @NotNull Integer folderId) {}
