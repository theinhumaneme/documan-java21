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
      @Index(name = "idx_file_subject_id", columnList = "subject_id"),
      @Index(name = "idx_file_folder_id", columnList = "folder_id")
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

  /**
   * The folder this file is filed under. Every file has one.
   *
   * <p>Mandatory rather than nullable: there is no such thing as a file loose at the root of a
   * subject. That removes a second place a file could be and a second listing to keep in step with
   * it, and it means "where is this filed?" always has an answer.
   *
   * <p>{@code subject} is kept alongside it rather than derived through the folder, because every
   * existing query and the search document reach for it directly and a join per file would be paid
   * on the hottest read in the application. The invariant is that the folder's subject and this one
   * are the same, which {@code FileService} maintains by only ever setting them together.
   */
  @JoinColumn(name = "folder_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Folder folder;
}
