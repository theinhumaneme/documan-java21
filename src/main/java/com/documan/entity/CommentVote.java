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
 * Explicit join entity replacing the {@code upvoted_comments} / {@code downvoted_comments} pair.
 */
@Entity
@Table(
    name = "comment_vote",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_comment_vote_comment_user",
            columnNames = {"comment_id", "user_id"}),
    indexes = {
      @Index(name = "idx_comment_vote_comment_id", columnList = "comment_id"),
      @Index(name = "idx_comment_vote_user_id", columnList = "user_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class CommentVote {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JoinColumn(name = "comment_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Comment comment;

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

  public CommentVote(Comment comment, User user, VoteType voteType) {
    this.comment = comment;
    this.user = user;
    this.voteType = voteType;
  }
}
