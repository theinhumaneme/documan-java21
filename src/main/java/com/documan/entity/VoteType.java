// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.entity;

/**
 * Direction of a vote. Replaces the free-form {@code String voteType} that previously flowed from
 * the controllers into the service layer, so an unknown value is rejected during request binding
 * rather than silently falling through to an empty result.
 */
public enum VoteType {
  UPVOTE,
  DOWNVOTE
}
