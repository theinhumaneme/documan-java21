// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.entity;

import com.documan.exception.InvalidRequestException;
import java.util.Arrays;

/**
 * The four roles and how they rank against each other.
 *
 * <p>Rank used to be the row's id — {@code regular} was 1, {@code moderator} 2, {@code admin} 3,
 * and {@code RoleService} compared those numbers directly. That worked only because the seed script
 * happened to insert them in order, and it broke the moment a fourth role existed: {@code
 * maintainer} belongs between regular and moderator, but an identity column can only append, so
 * seeding it made the narrowest role outrank the widest one. Ranking by name instead makes the id
 * what it should always have been — a surrogate key that no behaviour depends on — and lets the
 * seed order stay as it is.
 *
 * <p>A maintainer looks after the library: they upload and file material. A moderator looks after
 * what people write. Maintaining is the narrower job, so it sits lower, and everyone above a
 * maintainer can upload as well. This mirrors {@code ROLE_RANK} in the client's {@code api.ts}; the
 * two lists have to agree, because a gate the interface offers and the service refuses is worse
 * than either alone.
 */
public enum RoleName {
  REGULAR("regular", 0),
  MAINTAINER("maintainer", 1),
  MODERATOR("moderator", 2),
  ADMIN("admin", 3);

  private final String value;
  private final int rank;

  RoleName(String value, int rank) {
    this.value = value;
    this.rank = rank;
  }

  public String value() {
    return value;
  }

  public int rank() {
    return rank;
  }

  /**
   * The stored name to the role it means.
   *
   * <p>A row whose name is not one of these cannot be ranked, so it cannot be promoted to or from —
   * a 400 rather than an arbitrary ordering, because the alternative is a role that silently
   * behaves as though it outranks or underranks everything.
   */
  public static RoleName of(String name) {
    return Arrays.stream(values())
        .filter(role -> role.value.equalsIgnoreCase(name))
        .findFirst()
        .orElseThrow(
            () ->
                new InvalidRequestException(
                    "'%s' is not a role this service ranks".formatted(name)));
  }
}
