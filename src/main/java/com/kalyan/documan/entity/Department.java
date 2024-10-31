// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "department",
    indexes = {@Index(name = "idx_department_name", columnList = "name", unique = true)})
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "id")
public class Department {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @JsonIgnore
  @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
  @JsonManagedReference(value = "department-subjects")
  private List<Subject> subjects;

  @JsonIgnore
  @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
  @JsonManagedReference(value = "user-department")
  private List<User> users;
}
