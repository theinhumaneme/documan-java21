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
    name = "semester",
    indexes = {@Index(name = "idx_semester_name", columnList = "name", unique = true)})
@Getter
@Setter
public class Semester {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @JsonIgnore
  @OneToMany(mappedBy = "semester", fetch = FetchType.LAZY)
  @JsonManagedReference(value = "semester-subjects")
  private List<Subject> subjects;

  @JsonIgnore
  @OneToMany(mappedBy = "semester", fetch = FetchType.LAZY)
  @JsonManagedReference(value = "users-semester")
  private List<User> users;
}
