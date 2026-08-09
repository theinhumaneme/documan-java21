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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@EntityListeners(SearchEntityListener.class)
@Table(
    name = "comment",
    indexes = {
      @Index(name = "idx_comment_post_id", columnList = "post_id"),
      @Index(name = "idx_comment_user_id", columnList = "user_id"),
      @Index(name = "idx_comment_date_created", columnList = "date_created")
    })
@Getter
@Setter
public non-sealed class Comment implements Votable {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "upvote_count", nullable = false, columnDefinition = "bigint default 0")
  private long upvoteCount;

  @Column(name = "downvote_count", nullable = false, columnDefinition = "bigint default 0")
  private long downvoteCount;

  @Version
  @Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
  private long version;

  @Column(name = "date_created", nullable = false, updatable = false)
  @CreationTimestamp
  private OffsetDateTime dateCreated;

  @Column(name = "date_modified", nullable = false)
  @UpdateTimestamp
  private OffsetDateTime dateModified;

  @JoinColumn(name = "post_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Post post;

  @JoinColumn(name = "user_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private User user;
}
