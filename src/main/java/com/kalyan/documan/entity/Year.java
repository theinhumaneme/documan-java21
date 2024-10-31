// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "year",
    indexes = {@Index(name = "idx_year_name", columnList = "year", unique = true)})
@Getter
@Setter
public class Year {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "year", nullable = false, unique = true)
  private String value;

  @JsonIgnore
  @OneToMany(mappedBy = "year", fetch = FetchType.LAZY)
  @JsonManagedReference(value = "year-subjects")
  private List<Subject> subjects;

  @JsonIgnore
  @OneToMany(mappedBy = "year", fetch = FetchType.LAZY)
  @JsonManagedReference(value = "users-year")
  private List<User> users;
}
