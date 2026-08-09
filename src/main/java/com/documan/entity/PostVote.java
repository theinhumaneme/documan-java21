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

/**
 * Explicit join entity replacing the {@code upvoted_posts} / {@code downvoted_posts}
 * {@code @ManyToMany} pair. One row per (post, user); the direction lives in a column so switching
 * a vote is an update rather than a delete plus an insert across two tables.
 */
@Entity
@Table(
    name = "post_vote",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_post_vote_post_user",
            columnNames = {"post_id", "user_id"}),
    indexes = {
      @Index(name = "idx_post_vote_post_id", columnList = "post_id"),
      @Index(name = "idx_post_vote_user_id", columnList = "user_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class PostVote {

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

  @Enumerated(EnumType.STRING)
  @Column(name = "vote_type", nullable = false, length = 16)
  private VoteType voteType;

  @Column(name = "date_created", nullable = false, updatable = false)
  @CreationTimestamp
  private OffsetDateTime dateCreated;

  public PostVote(Post post, User user, VoteType voteType) {
    this.post = post;
    this.user = user;
    this.voteType = voteType;
  }
}
