// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "role",
    indexes = {@Index(name = "idx_role_name", columnList = "name", unique = true)})
@Getter
@Setter
public class Role {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "name", unique = true, nullable = false)
  private String name;

  @JsonIgnore
  @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
  @JsonManagedReference(value = "user-roles")
  private List<User> users;
}
