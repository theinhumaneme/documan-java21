// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "post",
    indexes = {
      @Index(name = "idx_post_title", columnList = "title"),
      @Index(name = "idx_post_user_id", columnList = "user_id")
    })
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "id")
public class Post {

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
  private Date dateCreated;

  @NotNull
  @Column(name = "date_modified", nullable = false)
  private Date dateModified;

  @JoinColumn(name = "user_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private User user;

  @OneToMany(mappedBy = "post")
  private List<Comment> comments;

  // users who have favourited the post
  @ManyToMany()
  @JoinTable(
      name = "favourite_posts",
      joinColumns = @JoinColumn(name = "post_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"),
      indexes = {
        @Index(name = "idx_favourite_posts_post_id", columnList = "post_id"),
        @Index(name = "idx_favourite_posts_user_id", columnList = "user_id")
      })
  private List<User> favouritedUsers;

  // Posts upvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "upvoted_posts",
      joinColumns = @JoinColumn(name = "post_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"),
      indexes = {
        @Index(name = "idx_upvoted_posts_post_id", columnList = "post_id"),
        @Index(name = "idx_upvoted_posts_user_id", columnList = "user_id")
      })
  private List<User> upvotedUsers;

  // Posts downvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "downvoted_posts",
      joinColumns = @JoinColumn(name = "post_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"),
      indexes = {
        @Index(name = "idx_downvoted_posts_post_id", columnList = "post_id"),
        @Index(name = "idx_downvoted_posts_user_id", columnList = "user_id")
      })
  private List<User> downvotedUsers;
}
