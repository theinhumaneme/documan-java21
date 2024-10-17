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
    name = "documan_user",
    indexes = {
      @Index(name = "idx_user_username", columnList = "username", unique = true),
      @Index(name = "idx_user_email", columnList = "email", unique = true),
      @Index(name = "idx_user_department_id  ", columnList = "department_id"),
      @Index(name = "idx_user_year_id", columnList = "year_id"),
      @Index(name = "idx_user_semester_id", columnList = "semester_id"),
      @Index(name = "idx_user_role_id", columnList = "role_id")
    })
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "id")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "username", unique = true, nullable = false)
  private String username;

  @NotNull
  @Column(name = "password", nullable = false)
  private String password;

  @NotNull
  @Column(name = "first_name", nullable = false)
  private String firstName;

  @NotNull
  @Column(name = "last_name")
  private String lastName;

  @NotNull
  @Column(name = "email", unique = true, nullable = false)
  private String email;

  @NotNull
  @Column(name = "terms_of_service", nullable = false)
  private boolean accptedTermsOfService = false;

  @NotNull
  @Column(name = "isVerified", nullable = false)
  private boolean isVerified = false;

  @NotNull
  @Column(name = "can_post", nullable = false)
  private boolean canPost = false;

  @NotNull
  @Column(name = "can_comment", nullable = false)
  private boolean canComment = false;

  @NotNull
  @Column(name = "date_created", nullable = false)
  private Date dateCreated;

  @NotNull
  @Column(name = "date_last_interacted", nullable = false)
  private Date dateLastInteracted;

  @JoinColumn(name = "role_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Role role;

  @JoinColumn(name = "department_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Department department;

  @JoinColumn(name = "year_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Year year;

  @JoinColumn(name = "semester_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Semester semester;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
  private ArrayList<Post> posts;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
  private ArrayList<Comment> comments;

  // Files favorite by the user
  @ManyToMany()
  @JoinTable(
      name = "favourite_files",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "file_id"),
      indexes = {
        @Index(name = "idx_favourite_files_user_id", columnList = "user_id"),
        @Index(name = "idx_favourite_files_file_id", columnList = "file_id")
      })
  private ArrayList<File> favouriteFiles;

  // Posts favorite by the user
  @ManyToMany()
  @JoinTable(
      name = "favourite_posts",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "post_id"),
      indexes = {
        @Index(name = "idx_favourite_posts_user_id", columnList = "user_id"),
        @Index(name = "idx_favourite_posts_post_id", columnList = "post_id")
      })
  private ArrayList<Post> favoritePosts;

  // Posts upvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "upvoted_posts",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "post_id"),
      indexes = {
        @Index(name = "idx_upvoted_posts_user_id", columnList = "user_id"),
        @Index(name = "idx_upvoted_posts_post_id", columnList = "post_id")
      })
  private ArrayList<Post> upvotedPosts;

  // Posts downvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "downvoted_posts",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "post_id"),
      indexes = {
        @Index(name = "idx_downvoted_posts_user_id", columnList = "user_id"),
        @Index(name = "idx_downvoted_posts_post_id", columnList = "post_id")
      })
  private ArrayList<Post> downvotedPosts;

  // Comments upvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "upvoted_comments",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "comment_id"),
      indexes = {
        @Index(name = "idx_upvoted_comments_user_id", columnList = "user_id"),
        @Index(name = "idx_upvoted_comments_comment_id", columnList = "comment_id")
      })
  private ArrayList<Comment> upvotedComments;

  // Comments downvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "downvoted_comments",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "comment_id"),
      indexes = {
        @Index(name = "idx_downvoted_comments_user_id", columnList = "user_id"),
        @Index(name = "idx_downvoted_comments_comment_id", columnList = "comment_id")
      })
  private ArrayList<Comment> downvotedComments;
}
