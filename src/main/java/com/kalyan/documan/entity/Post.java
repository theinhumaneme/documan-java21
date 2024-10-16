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
@Table(name = "post")
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "id")
public class Post {

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

  @JoinColumn(name = "user_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private User user;

  @OneToMany(mappedBy = "post")
  private ArrayList<Comment> comments;

  // users who have favorited the post
  @ManyToMany()
  @JoinTable(
      name = "favorite_posts",
      joinColumns = @JoinColumn(name = "post_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"))
  private ArrayList<User> favoritedUsers;

  // Posts upvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "upvoted_posts",
      joinColumns = @JoinColumn(name = "post_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"))
  private ArrayList<Post> upvotedUsers;

  // Posts downvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "downvoted_posts",
      joinColumns = @JoinColumn(name = "post_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"))
  private ArrayList<Post> downvotedUsers;
}
