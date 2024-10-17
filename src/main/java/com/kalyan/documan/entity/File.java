// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Date;
import java.util.ArrayList;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "file",
    indexes = {
      @Index(name = "idx_file_name", columnList = "name"),
      @Index(name = "idx_file_uuid", columnList = "uuid", unique = true),
      @Index(name = "idx_file_subject_id", columnList = "subject_id")
    })
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "id")
public class File {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  @NotNull
  @Column(name = "size", nullable = false)
  private Long size;

  @NotNull
  @Column(name = "uuid", unique = true, columnDefinition = "UUID", nullable = false)
  private UUID uuid = UUID.randomUUID();

  @NotNull
  @Column(name = "date_created", nullable = false)
  private Date dateCreated;

  @NotNull
  @Column(name = "date_modified", nullable = false)
  private Date dateModified;

  @JoinColumn(name = "subject_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Subject subject;

  @ManyToMany()
  @JoinTable(
      name = "favourite_files",
      joinColumns = @JoinColumn(name = "file_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"),
      indexes = {
        @Index(name = "idx_favourite_files_file_id", columnList = "file_id"),
        @Index(name = "idx_favourite_files_user_id", columnList = "user_id")
      })
  private ArrayList<User> favouritedUsers;
}
