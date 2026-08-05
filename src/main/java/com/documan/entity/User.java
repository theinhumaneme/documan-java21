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

  @NotNull
  @Column(name = "password", nullable = false)
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

  @JoinColumn(name = "department_id", nullable = false)
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  private Department department;

  @JoinColumn(name = "year_id", nullable = false)
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  private Year year;

  @JoinColumn(name = "semester_id", nullable = false)
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  private Semester semester;
}
