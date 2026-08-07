// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A folder inside one subject.
 *
 * <p>Deliberately not a tree. There is no parent, so a folder cannot be nested, reparented, or
 * moved into its own descendant — and the whole class of bugs that comes with those operations does
 * not exist. Files move between folders; folders stay where they are. The academic address
 * (department → year → semester → subject) already provides the hierarchy, and this is the one
 * level below it.
 *
 * <p>An integer identity key like every other entity here. A folder is addressed as {@code
 * ?folderId=12} internally and by {@code slug} in a readable URL — {@code /CSE/III/I/OS/unit-3} —
 * so a random key would buy nothing a caller can see while costing index locality.
 *
 * <p>{@code slug} is unique per subject, which is what stops two "Unit 3" folders appearing in one
 * subject. It is stable and machine-readable; {@code name} is what a reader sees and can be
 * changed.
 *
 * <p>{@code isDefault} marks the folders provisioned from {@link DefaultFolder}. They cannot be
 * renamed or deleted, so the unit structure a subject was created with stays intact, while custom
 * folders a maintainer adds are fully theirs to manage.
 *
 * <p>Not registered with {@code SearchEntityListener}: folders are not indexed, and no field of a
 * folder is denormalised into a file's search document, so renaming one cannot leave the index
 * stale. Registering it would enqueue outbox rows that drain to nothing.
 */
@Entity
@Table(
    name = "folder",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_folder_subject_slug",
            columnNames = {"subject_id", "slug"}),
    indexes = {@Index(name = "idx_folder_subject_id", columnList = "subject_id")})
@Getter
@Setter
public class Folder {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  @NotNull
  @Column(name = "slug", nullable = false)
  private String slug;

  /**
   * Named without the {@code is} prefix and mapped to {@code is_default}, matching {@code
   * Subject.lab} → {@code is_lab}. A field literally called {@code isDefault} makes Lombok's
   * accessor names and MapStruct's derived property name disagree, which fails at build time in a
   * way that reads as a mapping error rather than a naming one.
   */
  @Column(name = "is_default", nullable = false)
  private boolean defaultFolder;

  @Version
  @Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
  private long version;

  @Column(name = "date_created", nullable = false, updatable = false)
  @CreationTimestamp
  private OffsetDateTime dateCreated;

  @Column(name = "date_modified", nullable = false)
  @UpdateTimestamp
  private OffsetDateTime dateModified;

  @JoinColumn(name = "subject_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Subject subject;
}
