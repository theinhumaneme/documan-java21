// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "subject",
    indexes = {
      @Index(name = "idx_subject_code", columnList = "code"),
      @Index(name = "idx_subject_department_id  ", columnList = "department_id"),
      @Index(name = "idx_subject_year_id", columnList = "year_id"),
      @Index(name = "idx_subject_semester_id", columnList = "semester_id")
    })
@Getter
@Setter
public class Subject {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "name")
  private String name;

  @NotNull
  @Column(name = "is_lab")
  private boolean isLab;

  @NotNull
  @Column(name = "is_theory")
  private boolean isTheory;

  @NotNull
  @Column(name = "code")
  private String code;

  @JsonIgnore
  @JoinColumn(name = "department_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference(value = "department-subjects")
  private Department department;

  @JsonIgnore
  @JoinColumn(name = "year_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference(value = "year-subjects")
  private Year year;

  @JsonIgnore
  @JoinColumn(name = "semester_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference(value = "semester-subjects")
  private Semester semester;

  @JsonIgnore
  @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
  @JsonManagedReference(value = "subject-files")
  private List<File> files;
}
