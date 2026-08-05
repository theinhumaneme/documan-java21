// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.document;

/**
 * Carries {@code postId} but deliberately not the post title: embedding it would add a second
 * fan-out (editing a post would touch every comment on it) for no search value.
 */
public record CommentDocument(
    Integer id,
    String content,
    Integer postId,
    Integer authorId,
    String authorUsername,
    long upvoteCount,
    long downvoteCount,
    long dateCreated) {}
