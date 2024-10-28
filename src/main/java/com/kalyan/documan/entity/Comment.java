// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "comment",
    indexes = {
      @Index(name = "idx_comment_title", columnList = "title"),
      @Index(name = "idx_comment_post_id", columnList = "post_id"),
      @Index(name = "idx_comment_user_id  ", columnList = "user_id")
    })
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "id")
public class Comment {

  @Id
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

  @NotNull
  @Column(name = "date_created", nullable = false)
  @CreationTimestamp
  private Date dateCreated;

  @NotNull
  @Column(name = "date_modified", nullable = false)
  @UpdateTimestamp
  private Date dateModified;

  @JsonIgnore
  @JoinColumn(name = "post_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Post post;

  @JsonIgnore
  @JoinColumn(name = "user_id", nullable = false)
  @ManyToOne(fetch = FetchType.EAGER)
  private User user;

  // Comments upvoted by the user
  @JsonIgnore
  @ManyToMany()
  @JoinTable(
      name = "upvoted_comments",
      joinColumns = @JoinColumn(name = "comment_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"),
      indexes = {
        @Index(name = "idx_upvoted_comments_comment_id", columnList = "comment_id"),
        @Index(name = "idx_upvoted_comments_user_id", columnList = "user_id")
      })
  private List<User> upvotedUsers;

  // Comments downvoted by the user
  @JsonIgnore
  @ManyToMany()
  @JoinTable(
      name = "downvoted_comments",
      joinColumns = @JoinColumn(name = "comment_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"),
      indexes = {
        @Index(name = "idx_downvoted_comments_comment_id", columnList = "comment_id"),
        @Index(name = "idx_downvoted_comments_user_id", columnList = "user_id")
      })
  private List<User> downvotedUsers;
}
