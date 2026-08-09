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
 * @param folderId the folder this file is filed under; every file has one
 */
public record FileResponse(
    Integer id,
    String name,
    String objectName,
    String objectURL,
    Long size,
    long favouriteCount,
    Integer subjectId,
    Integer folderId,
    OffsetDateTime dateCreated,
    OffsetDateTime dateModified)
    implements Serializable {}
