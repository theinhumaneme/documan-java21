// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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
  @Column(name = "terms_of_service", nullable = false, columnDefinition = "boolean DEFAULT false")
  private boolean acceptedTermsOfService;

  @NotNull
  @Column(name = "isVerified", nullable = false, columnDefinition = "boolean DEFAULT false")
  private boolean isVerified;

  @NotNull
  @Column(name = "can_post", nullable = false, columnDefinition = "boolean DEFAULT false")
  private boolean canPost;

  @NotNull
  @Column(name = "can_comment", nullable = false, columnDefinition = "boolean DEFAULT false")
  private boolean canComment;

  @NotNull
  @Column(name = "date_created", nullable = false)
  @CreationTimestamp
  private OffsetDateTime dateCreated;

  @NotNull
  @Column(name = "date_last_interacted", nullable = false)
  @UpdateTimestamp
  private OffsetDateTime dateLastInteracted;

  @JoinColumn(name = "role_id", nullable = false)
  @ManyToOne(fetch = FetchType.EAGER)
  @JsonBackReference(value = "user-roles")
  private Role role;

  @JoinColumn(name = "department_id", nullable = false)
  @ManyToOne(fetch = FetchType.EAGER)
  @JsonBackReference(value = "user-department")
  private Department department;

  @JoinColumn(name = "year_id", nullable = false)
  @ManyToOne(fetch = FetchType.EAGER)
  @JsonBackReference(value = "users-year")
  private Year year;

  @JoinColumn(name = "semester_id", nullable = false)
  @ManyToOne(fetch = FetchType.EAGER)
  @JsonBackReference(value = "users-semester")
  private Semester semester;

  @JsonIgnore
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
  @JsonManagedReference(value = "user-posts")
  private List<Post> posts;

  @JsonIgnore
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
  @JsonManagedReference(value = "user-comments")
  private List<Comment> comments;

  // Files favorite by the user
  @JsonIgnore
  @ManyToMany()
  @JoinTable(
      name = "favourite_files",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "file_id"),
      indexes = {
        @Index(name = "idx_favourite_files_user_id", columnList = "user_id"),
        @Index(name = "idx_favourite_files_file_id", columnList = "file_id")
      })
  private List<File> favouriteFiles;

  // Posts favorite by the user
  @JsonIgnore
  @ManyToMany()
  @JoinTable(
      name = "favourite_posts",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "post_id"),
      indexes = {
        @Index(name = "idx_favourite_posts_user_id", columnList = "user_id"),
        @Index(name = "idx_favourite_posts_post_id", columnList = "post_id")
      })
  private List<Post> favoritePosts;

  // Posts upvoted by the user
  @JsonIgnore
  @ManyToMany()
  @JoinTable(
      name = "upvoted_posts",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "post_id"),
      indexes = {
        @Index(name = "idx_upvoted_posts_user_id", columnList = "user_id"),
        @Index(name = "idx_upvoted_posts_post_id", columnList = "post_id")
      })
  private List<Post> upvotedPosts;

  // Posts downvoted by the user
  @JsonIgnore
  @ManyToMany()
  @JoinTable(
      name = "downvoted_posts",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "post_id"),
      indexes = {
        @Index(name = "idx_downvoted_posts_user_id", columnList = "user_id"),
        @Index(name = "idx_downvoted_posts_post_id", columnList = "post_id")
      })
  private List<Post> downvotedPosts;

  // Comments upvoted by the user
  @JsonIgnore
  @ManyToMany()
  @JoinTable(
      name = "upvoted_comments",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "comment_id"),
      indexes = {
        @Index(name = "idx_upvoted_comments_user_id", columnList = "user_id"),
        @Index(name = "idx_upvoted_comments_comment_id", columnList = "comment_id")
      })
  private List<Comment> upvotedComments;

  // Comments downvoted by the user
  @JsonIgnore
  @ManyToMany()
  @JoinTable(
      name = "downvoted_comments",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "comment_id"),
      indexes = {
        @Index(name = "idx_downvoted_comments_user_id", columnList = "user_id"),
        @Index(name = "idx_downvoted_comments_comment_id", columnList = "comment_id")
      })
  private List<Comment> downvotedComments;
}
