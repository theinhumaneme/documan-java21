// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.document;

/** {@code content} is truncated; the full body is served from PostgreSQL. */
public record PostDocument(
    Integer id,
    String title,
    String description,
    String content,
    Integer authorId,
    String authorUsername,
    long upvoteCount,
    long downvoteCount,
    long favouriteCount,
    long netScore,
    long dateCreated,
    long dateModified) {}
