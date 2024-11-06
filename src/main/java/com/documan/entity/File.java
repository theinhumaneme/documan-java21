// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "file",
    indexes = {
      @Index(name = "idx_file_name", columnList = "name"),
      @Index(name = "idx_file_object_name", columnList = "object_name"),
      @Index(name = "idx_file_object_url", columnList = "object_url"),
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
  @Column(name = "object_name", nullable = false)
  private String objectName;

  @NotNull
  @Column(name = "object_url", nullable = false)
  private String objectURL;

  @NotNull
  @Column(name = "size", nullable = false)
  private Long size;

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
