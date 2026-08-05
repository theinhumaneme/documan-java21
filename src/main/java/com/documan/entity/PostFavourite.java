// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Explicit join entity replacing the {@code favourite_posts} {@code @ManyToMany}. */
@Entity
@Table(
    name = "favourite_post",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_favourite_post_post_user",
            columnNames = {"post_id", "user_id"}),
    indexes = {
      @Index(name = "idx_favourite_post_post_id", columnList = "post_id"),
      @Index(name = "idx_favourite_post_user_id", columnList = "user_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class PostFavourite {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JoinColumn(name = "post_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Post post;

  @JoinColumn(name = "user_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private User user;

  @Column(name = "date_created", nullable = false, updatable = false)
  @CreationTimestamp
  private OffsetDateTime dateCreated;

  public PostFavourite(Post post, User user) {
    this.post = post;
    this.user = user;
  }
}
