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
 * The owning side of every user relationship now lives in a dedicated join entity or is reached
 * through a paginated repository query. The previous {@code @ManyToMany List<Post>} collections for
 * votes and favourites meant a single vote had to materialise every voter, so they were removed
 * rather than made lazy.
 */
@Entity
@Table(
    name = "documan_user",
    indexes = {
      @Index(name = "idx_user_username", columnList = "username", unique = true),
      @Index(name = "idx_user_email", columnList = "email", unique = true),
      @Index(name = "idx_user_external_id", columnList = "external_id", unique = true),
      @Index(name = "idx_user_department_id", columnList = "department_id"),
      @Index(name = "idx_user_year_id", columnList = "year_id"),
      @Index(name = "idx_user_semester_id", columnList = "semester_id"),
      @Index(name = "idx_user_role_id", columnList = "role_id")
    })
@Getter
@Setter
public class User {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "username", unique = true, nullable = false)
  private String username;

  /**
   * The identity provider's own id for this person — Clerk's {@code sub} — and the only claim safe
   * to key a row on. A username and an email address are both things someone can change; changing
   * either would strand the row and silently provision a second one at the next sign-in.
   *
   * <p>Named for the role rather than the provider, because this is the second provider it has held
   * and the column should not have to be renamed for a third.
   *
   * <p>Null on rows that predate the provider, which is why the unique index is the constraint
   * rather than {@code nullable = false}: PostgreSQL does not consider two nulls equal, so those
   * rows coexist and can be claimed later without a migration.
   */
  @Column(name = "external_id", unique = true)
  private String externalId;

  /**
   * Null for every account that signs in through Clerk, which after the migration is all of them.
   * The provider holds the credential and this service never sees one; the column stays for rows
   * created before it and goes when they do.
   */
  @Column(name = "password")
  private String password;

  @NotNull
  @Column(name = "first_name", nullable = false)
  private String firstName;

  @NotNull
  @Column(name = "last_name", nullable = false)
  private String lastName;

  @NotNull
  @Column(name = "email", unique = true, nullable = false)
  private String email;

  @Column(name = "terms_of_service", nullable = false, columnDefinition = "boolean default false")
  private boolean acceptedTermsOfService;

  @Column(name = "is_verified", nullable = false, columnDefinition = "boolean default false")
  private boolean verified;

  @Column(name = "can_post", nullable = false, columnDefinition = "boolean default false")
  private boolean canPost;

  @Column(name = "can_comment", nullable = false, columnDefinition = "boolean default false")
  private boolean canComment;

  @Version
  @Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
  private long version;

  @Column(name = "date_created", nullable = false, updatable = false)
  @CreationTimestamp
  private OffsetDateTime dateCreated;

  @Column(name = "date_last_interacted", nullable = false)
  @UpdateTimestamp
  private OffsetDateTime dateLastInteracted;

  @JoinColumn(name = "role_id", nullable = false)
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  private Role role;

  /**
   * Where the reader sits in the course structure. All three are null on an account just
   * provisioned from a token, and stay so until the registration form files them: the identity
   * provider knows who someone is, not which department, year and semester they study in.
   *
   * <p>They were {@code nullable = false}, which is what a self-registration form could guarantee
   * and just-in-time provisioning cannot. Nothing reads them for authorisation — they scope the
   * library view — so a null is a reader who has not chosen yet, not a broken row.
   */
  @JoinColumn(name = "department_id")
  @ManyToOne(fetch = FetchType.EAGER)
  private Department department;

  @JoinColumn(name = "year_id")
  @ManyToOne(fetch = FetchType.EAGER)
  private Year year;

  @JoinColumn(name = "semester_id")
  @ManyToOne(fetch = FetchType.EAGER)
  private Semester semester;
}
