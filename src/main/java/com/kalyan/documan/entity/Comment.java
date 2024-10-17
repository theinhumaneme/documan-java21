/* Copyright (C)2024 Mudumby Kalyan / @theinhumaneme  */
package com.kalyan.documan.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Date;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

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
  @Column(name = "title", columnDefinition = "TEXT", nullable = false)
  private String title;

  @NotNull
  @Column(name = "description", nullable = false)
  private String description;

  @NotNull
  @Column(name = "content", columnDefinition = "TEXT", nullable = false)
  private String content;

  @NotNull
  @Column(name = "date_created", nullable = false)
  private Date dateCreated;

  @NotNull
  @Column(name = "date_modified", nullable = false)
  private Date dateModified;

  @JoinColumn(name = "post_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Post post;

  @JoinColumn(name = "user_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private User user;

  // Comments upvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "upvoted_comments",
      joinColumns = @JoinColumn(name = "comment_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"),
      indexes = {
        @Index(name = "idx_upvoted_comments_comment_id", columnList = "comment_id"),
        @Index(name = "idx_upvoted_comments_user_id", columnList = "user_id")
      })
  private ArrayList<User> upvotedUsers;

  // Comments downvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "downvoted_comments",
      joinColumns = @JoinColumn(name = "comment_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"),
      indexes = {
        @Index(name = "idx_downvoted_comments_comment_id", columnList = "comment_id"),
        @Index(name = "idx_downvoted_comments_user_id", columnList = "user_id")
      })
  private ArrayList<User> downvotedUsers;
}
