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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
  @Column(
      name = "uuid",
      unique = true,
      nullable = false,
      columnDefinition = "UUID DEFAULT gen_random_uuid()")
  private UUID uuid = UUID.randomUUID();

  @NotNull
  @Column(name = "date_created", nullable = false)
  @CreationTimestamp
  private OffsetDateTime dateCreated;

  @NotNull
  @Column(name = "date_modified", nullable = false)
  @UpdateTimestamp
  private OffsetDateTime dateModified;

  @JsonIgnore
  @JoinColumn(name = "subject_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference(value = "subject-files")
  private Subject subject;

  @JsonIgnore
  @ManyToMany()
  @JoinTable(
      name = "favourite_files",
      joinColumns = @JoinColumn(name = "file_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"),
      indexes = {
        @Index(name = "idx_favourite_files_file_id", columnList = "file_id"),
        @Index(name = "idx_favourite_files_user_id", columnList = "user_id")
      })
  private List<User> favouritedUsers;
}
