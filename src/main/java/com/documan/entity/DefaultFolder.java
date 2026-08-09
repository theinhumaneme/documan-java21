// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * The folders a subject can be created with.
 *
 * <p>Offered rather than imposed. A subject is created with whichever of these the caller asks for
 * and nothing else, so a subject that does not follow the unit structure is not left with five
 * empty folders nobody will file anything into.
 *
 * <p>There is deliberately no "Other files" entry. A file's folder is nullable, so a file that
 * belongs to no particular part of the course simply sits at the root of its subject — the same
 * place a file sits in a Drive before anyone organises it. An explicit bucket for "everything else"
 * would be a second way to express the same state.
 *
 * <p>{@link #LAB} is only meaningful for a subject with a lab. Nothing here enforces that: the
 * service refuses to provision it for a theory-only subject, and the client hides it if the subject
 * later stops having a lab. Hiding rather than deleting is the point — switching {@code is_lab} off
 * is a metadata edit and must not destroy uploaded material.
 */
public enum DefaultFolder {
  COURSEFILES("coursefiles", "Coursefiles", false),
  UNIT_1("unit-1", "Unit 1", false),
  UNIT_2("unit-2", "Unit 2", false),
  UNIT_3("unit-3", "Unit 3", false),
  UNIT_4("unit-4", "Unit 4", false),
  UNIT_5("unit-5", "Unit 5", false),
  LAB("lab", "Lab", true);

  private final String slug;
  private final String displayName;
  private final boolean requiresLab;

  DefaultFolder(String slug, String displayName, boolean requiresLab) {
    this.slug = slug;
    this.displayName = displayName;
    this.requiresLab = requiresLab;
  }

  public String slug() {
    return slug;
  }

  public String displayName() {
    return displayName;
  }

  /** True when this folder only makes sense for a subject whose {@code is_lab} is set. */
  public boolean requiresLab() {
    return requiresLab;
  }

  public static Optional<DefaultFolder> bySlug(String slug) {
    return Arrays.stream(values()).filter(folder -> folder.slug.equals(slug)).findFirst();
  }
}
