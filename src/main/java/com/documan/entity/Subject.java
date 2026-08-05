// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.entity;

import com.documan.search.outbox.SearchEntityListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@EntityListeners(SearchEntityListener.class)
@Table(
    name = "subject",
    indexes = {
      @Index(name = "idx_subject_code", columnList = "code"),
      @Index(name = "idx_subject_department_id", columnList = "department_id"),
      @Index(name = "idx_subject_year_id", columnList = "year_id"),
      @Index(name = "idx_subject_semester_id", columnList = "semester_id"),
      @Index(
          name = "idx_subject_department_year_semester",
          columnList = "department_id, year_id, semester_id")
    })
@Getter
@Setter
public class Subject {

  /** Was {@code Long} while {@code SubjectDao} declared {@code Integer}; unified on Integer. */
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "is_lab", nullable = false)
  private boolean lab;

  @Column(name = "is_theory", nullable = false)
  private boolean theory;

  @NotNull
  @Column(name = "code", nullable = false)
  private String code;

  @Version
  @Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
  private long version;

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
