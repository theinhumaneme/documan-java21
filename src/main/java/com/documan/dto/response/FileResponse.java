// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.response;

import java.io.Serializable;
import java.time.OffsetDateTime;

public record FileResponse(
    Integer id,
    String name,
    String objectName,
    String objectURL,
    Long size,
    long favouriteCount,
    Integer subjectId,
    OffsetDateTime dateCreated,
    OffsetDateTime dateModified)
    implements Serializable {}
