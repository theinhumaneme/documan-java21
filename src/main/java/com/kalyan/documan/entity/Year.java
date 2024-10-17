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
    name = "year",
    indexes = {@Index(name = "idx_year_name", columnList = "year", unique = true)})
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "id")
public class Year {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "year", nullable = false, unique = true)
  private String value;

  @OneToMany(mappedBy = "year", fetch = FetchType.LAZY)
  private ArrayList<Semester> semesters;

  @OneToMany(mappedBy = "year", fetch = FetchType.LAZY)
  private ArrayList<Subject> subjects;
}
