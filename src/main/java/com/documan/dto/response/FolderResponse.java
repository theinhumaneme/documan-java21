// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.response;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * @param slug stable, machine-readable, unique within the subject; what a readable URL uses
 * @param isDefault provisioned from {@code DefaultFolder}; cannot be renamed or deleted
 * @param fileCount how many files are filed here, so a folder grid does not need a request per
 *     folder to show it
 */
public record FolderResponse(
    Integer id,
    String name,
    String slug,
    boolean isDefault,
    Integer subjectId,
    long fileCount,
    OffsetDateTime dateCreated,
    OffsetDateTime dateModified)
    implements Serializable {}
