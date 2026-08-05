// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.response;

import java.io.Serializable;
import java.time.OffsetDateTime;

public record PostResponse(
    Integer id,
    String title,
    String description,
    String content,
    long upvoteCount,
    long downvoteCount,
    long favouriteCount,
    Integer authorId,
    String authorUsername,
    OffsetDateTime dateCreated,
    OffsetDateTime dateModified)
    implements Serializable {}
