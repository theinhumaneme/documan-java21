// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * One grant: how much of the library a maintainer may change.
 *
 * <p>The three columns narrow from left to right, and each may be null, which means "all of them".
 * That gives the three levels a grant is actually made at without three tables or a level
 * discriminator to keep honest:
 *
 * <ul>
 *   <li>{@code (CSE, null, null)} — the whole department
 *   <li>{@code (CSE, III, null)} — one year of it
 *   <li>{@code (CSE, III, I)} — one semester
 * </ul>
 *
 * <p>Null-as-wildcard rather than a row per semester because the grants are made by hand and read
 * by a person: "all of CSE" is one row that says so, not twelve rows that have to be recognised
 * collectively as covering everything. It is also stable when a year or semester is added later —
 * an enumerated grant would silently fail to cover the new one.
 *
 * <p>A narrower column cannot be set without the ones before it: a semester with no year would
 * describe "semester I of every year of this department", which is not a thing anyone means, and
 * would make the check ambiguous. {@code MaintainerScopeService} refuses it.
 *
 * <p><strong>This shapes the interface; it does not secure anything.</strong> The application has
 * no authentication — {@code WebSecurityConfig} still permits every request — so nothing here can
 * be enforced against a caller who does not go through the client. The rows are real and the checks
 * are written; what is missing is a principal to check them against. When the Entra ID resource
 * server lands, this becomes the authorisation rule and needs no other change.
 */
@Entity
@Table(
    name = "maintainer_scope",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_maintainer_scope_grant",
            columnNames = {"user_id", "department_id", "year_id", "semester_id"}),
    indexes = {@Index(name = "idx_maintainer_scope_user_id", columnList = "user_id")})
@Getter
@Setter
public class MaintainerScope {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @JoinColumn(name = "user_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private User user;

  @JoinColumn(name = "department_id", nullable = false)
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  private Department department;

  /** Null grants every year of the department. */
  @JoinColumn(name = "year_id")
  @ManyToOne(fetch = FetchType.EAGER)
  private Year year;

  /** Null grants every semester of the year. Only meaningful when a year is set. */
  @JoinColumn(name = "semester_id")
  @ManyToOne(fetch = FetchType.EAGER)
  private Semester semester;

  @Column(name = "date_created", nullable = false, updatable = false)
  @CreationTimestamp
  private OffsetDateTime dateCreated;

  /**
   * Whether this grant covers an address.
   *
   * <p>A null column matches anything, so the comparison is only made for the columns the grant
   * actually names. Kept on the entity rather than in a query so that the rule is written once and
   * reads the same wherever it is applied.
   */
  public boolean covers(Integer departmentId, Integer yearId, Integer semesterId) {
    if (!department.getId().equals(departmentId)) {
      return false;
    }
    if (year != null && !year.getId().equals(yearId)) {
      return false;
    }
    return semester == null || semester.getId().equals(semesterId);
  }
}
