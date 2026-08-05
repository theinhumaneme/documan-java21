// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.entity;

/**
 * Entities that carry denormalised vote tallies. Sealing the hierarchy lets the vote service
 * dispatch with an exhaustive pattern-matching switch instead of the unchecked generic casts the
 * previous implementation relied on.
 */
public sealed interface Votable permits Post, Comment {

  Integer getId();

  long getUpvoteCount();

  long getDownvoteCount();
}
