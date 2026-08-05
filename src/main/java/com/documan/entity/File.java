// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.entity;

import com.documan.search.outbox.SearchEntityListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@EntityListeners(SearchEntityListener.class)
@Table(
    name = "file",
    indexes = {
      @Index(name = "idx_file_name", columnList = "name"),
      @Index(name = "idx_file_object_name", columnList = "object_name", unique = true),
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
  @Column(name = "object_name", nullable = false, unique = true)
  private String objectName;

  @NotNull
  @Column(name = "object_url", nullable = false)
  private String objectURL;

  @NotNull
  @Column(name = "size", nullable = false)
  private Long size;

  @Column(name = "favourite_count", nullable = false, columnDefinition = "bigint default 0")
  private long favouriteCount;

  @Version
  @Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
  private long version;

  @Column(name = "date_created", nullable = false, updatable = false)
  @CreationTimestamp
  private OffsetDateTime dateCreated;

  @Column(name = "date_modified", nullable = false)
  @UpdateTimestamp
  private OffsetDateTime dateModified;

  @JoinColumn(name = "subject_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Subject subject;
}
