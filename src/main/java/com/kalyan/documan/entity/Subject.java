/* Copyright (C)2024 Mudumby Kalyan / @theinhumaneme  */
package com.kalyan.documan.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
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
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "id")
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

  @JoinColumn(name = "department_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Department department;

  @JoinColumn(name = "year_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Year year;

  @JoinColumn(name = "semester_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Semester semester;

  @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
  private ArrayList<File> files;
}