// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.entity.VoteType;

/**
 * How a vote transition moves the two denormalised tallies. The switches are exhaustive over {@link
 * VoteType}, so adding a third direction becomes a compile error rather than a silent fallthrough.
 */
record VoteDelta(long up, long down) {

  static VoteDelta added(VoteType type) {
    return switch (type) {
      case UPVOTE -> new VoteDelta(1, 0);
      case DOWNVOTE -> new VoteDelta(0, 1);
    };
  }

  static VoteDelta removed(VoteType type) {
    return switch (type) {
      case UPVOTE -> new VoteDelta(-1, 0);
      case DOWNVOTE -> new VoteDelta(0, -1);
    };
  }

  /** A caller flipping an existing vote moves one tally up and the other down. */
  static VoteDelta switchedTo(VoteType type) {
    return switch (type) {
      case UPVOTE -> new VoteDelta(1, -1);
      case DOWNVOTE -> new VoteDelta(-1, 1);
    };
  }
}
