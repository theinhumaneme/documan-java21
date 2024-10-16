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
@Table(name = "documan_user")
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

  // Subjects favorite by the user
  @ManyToMany()
  @JoinTable(
      name = "favorite_subjects",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "subject_id"))
  private ArrayList<Subject> favoriteSubjects;

  // Files favorite by the user
  @ManyToMany()
  @JoinTable(
      name = "favorite_files",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "file_id"))
  private ArrayList<Post> favoriteFiles;

  // Posts favorite by the user
  @ManyToMany()
  @JoinTable(
      name = "favorite_posts",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "post_id"))
  private ArrayList<Post> favoritePosts;

  // Posts upvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "upvoted_posts",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "post_id"))
  private ArrayList<Post> upvotedPosts;

  // Posts downvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "downvoted_posts",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "post_id"))
  private ArrayList<Post> downvotedPost;

  // Comments upvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "upvoted_comments",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "comment_id"))
  private ArrayList<Comment> upvotedComments;

  // Comments downvoted by the user
  @ManyToMany()
  @JoinTable(
      name = "downvoted_comments",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "comment_id"))
  private ArrayList<Comment> downvotedComments;
}
