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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@EntityListeners(SearchEntityListener.class)
@Table(
    name = "post",
    indexes = {
      @Index(name = "idx_post_title", columnList = "title"),
      @Index(name = "idx_post_user_id", columnList = "user_id"),
      @Index(name = "idx_post_date_created", columnList = "date_created")
    })
@Getter
@Setter
public non-sealed class Post implements Votable {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "title", nullable = false, columnDefinition = "TEXT")
  private String title;

  @NotNull
  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @NotNull
  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  /**
   * Denormalised tallies. Votes are applied with atomic {@code UPDATE ... SET count = count + ?}
   * statements so casting a single vote never loads the whole voter collection.
   */
  @Column(name = "upvote_count", nullable = false, columnDefinition = "bigint default 0")
  private long upvoteCount;

  @Column(name = "downvote_count", nullable = false, columnDefinition = "bigint default 0")
  private long downvoteCount;

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

  @JoinColumn(name = "user_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private User user;

  @BatchSize(size = 50)
  @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private List<Comment> comments = new ArrayList<>();
}
