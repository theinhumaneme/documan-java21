// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.documan.AbstractDataTest;
import com.documan.dto.request.CreateUserRequest;
import com.documan.dto.request.UpdateUserRequest;
import com.documan.dto.response.PageResponse;
import com.documan.dto.response.PostResponse;
import com.documan.dto.response.UserResponse;
import com.documan.entity.Post;
import com.documan.entity.User;
import com.documan.entity.VoteType;
import com.documan.exception.DuplicateResourceException;
import com.documan.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class PostAndUserServiceTest extends AbstractDataTest {

  @Autowired private PostService postService;
  @Autowired private UserService userService;
  @Autowired private VoteService voteService;
  @Autowired private FavouriteService favouriteService;

  @Test
  void listingPostsIsPagedRatherThanUnbounded() {
    User author = newUser("author");
    for (int i = 0; i < 25; i++) {
      newPost(author, "title-" + i);
    }

    PageResponse<PostResponse> firstPage = postService.findAll(PageRequest.of(0, 10));

    assertThat(firstPage.content()).hasSize(10);
    assertThat(firstPage.totalElements()).isEqualTo(25);
    assertThat(firstPage.totalPages()).isEqualTo(3);
    assertThat(firstPage.first()).isTrue();
    assertThat(firstPage.last()).isFalse();
  }

  @Test
  void postResponsesCarryTheAuthorWithoutALazyLoadFailure() {
    User author = newUser("author");
    newPost(author, "title");

    PostResponse response = postService.findAll(PageRequest.of(0, 10)).content().getFirst();

    assertThat(response.authorId()).isEqualTo(author.getId());
    assertThat(response.authorUsername()).isEqualTo("author");
  }

  @Test
  void postsOfAUserWithNoPostsIsAnEmptyPageNotAnError() {
    User user = newUser("quiet");

    PageResponse<PostResponse> page = postService.findByUser(user.getId(), PageRequest.of(0, 10));

    assertThat(page.content()).isEmpty();
    assertThat(page.totalElements()).isZero();
  }

  @Test
  void listingPostsOfAMissingUserReports404() {
    assertThatThrownBy(() -> postService.findByUser(9999, PageRequest.of(0, 10)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deletingAPostRemovesItsComments() {
    User author = newUser("author");
    Post post = newPost(author, "title");
    newComment(author, post, "a comment");
    assertThat(commentDao.count()).isEqualTo(1);

    postService.delete(post.getId());

    assertThat(postDao.count()).isZero();
    assertThat(commentDao.count()).isZero();
  }

  /** The previous implementation overwrote the stored password on every profile update. */
  @Test
  void updatingAProfileWithoutAPasswordLeavesTheCredentialAlone() {
    UserResponse created =
        userService.create(
            new CreateUserRequest(
                "student",
                "initial-password",
                "First",
                "Last",
                "student@example.test",
                department.getId(),
                year.getId(),
                semester.getId(),
                true));
    String storedBefore = userDao.findById(created.id()).orElseThrow().getPassword();

    userService.update(
        created.id(),
        new UpdateUserRequest(
            "student",
            null,
            "Changed",
            "Last",
            "student@example.test",
            department.getId(),
            year.getId(),
            semester.getId(),
            null,
            null,
            null));

    User after = userDao.findById(created.id()).orElseThrow();
    assertThat(after.getPassword()).isEqualTo(storedBefore);
    assertThat(after.getFirstName()).isEqualTo("Changed");
  }

  /** Create/update previously ignored these flags entirely. */
  @Test
  void flagsFromTheRequestArePersisted() {
    UserResponse created =
        userService.create(
            new CreateUserRequest(
                "flagged",
                "initial-password",
                "First",
                "Last",
                "flagged@example.test",
                department.getId(),
                year.getId(),
                semester.getId(),
                true));
    assertThat(created.acceptedTermsOfService()).isTrue();

    UserResponse updated =
        userService.update(
            created.id(),
            new UpdateUserRequest(
                "flagged",
                null,
                "First",
                "Last",
                "flagged@example.test",
                department.getId(),
                year.getId(),
                semester.getId(),
                true,
                true,
                true));

    assertThat(updated.canPost()).isTrue();
    assertThat(updated.canComment()).isTrue();
  }

  @Test
  void duplicateUsernameIsRejectedAsAConflict() {
    newUser("taken");

    assertThatThrownBy(
            () ->
                userService.create(
                    new CreateUserRequest(
                        "taken",
                        "initial-password",
                        "First",
                        "Last",
                        "other@example.test",
                        department.getId(),
                        year.getId(),
                        semester.getId(),
                        true)))
        .isInstanceOf(DuplicateResourceException.class);
  }

  @Test
  void userResponseNeverExposesThePassword() {
    User user = newUser("private");

    UserResponse response = userService.findById(user.getId());

    assertThat(response).hasNoNullFieldsOrPropertiesExcept();
    assertThat(response.toString()).doesNotContain("placeholder-password");
  }

  @Test
  void relationshipViewsArePagedAndFilterByDirection() {
    User voter = newUser("voter");
    Post liked = newPost(newUser("author"), "liked");
    Post disliked = newPost(newUser("author2"), "disliked");

    voteService.votePost(liked.getId(), voter.getId(), VoteType.UPVOTE);
    voteService.votePost(disliked.getId(), voter.getId(), VoteType.DOWNVOTE);
    favouriteService.favouritePost(liked.getId(), voter.getId());

    assertThat(
            userService
                .findVotedPosts(voter.getId(), VoteType.UPVOTE, PageRequest.of(0, 10))
                .content())
        .singleElement()
        .satisfies(post -> assertThat(post.title()).isEqualTo("liked"));

    assertThat(
            userService
                .findVotedPosts(voter.getId(), VoteType.DOWNVOTE, PageRequest.of(0, 10))
                .content())
        .singleElement()
        .satisfies(post -> assertThat(post.title()).isEqualTo("disliked"));

    assertThat(userService.findFavouritePosts(voter.getId(), PageRequest.of(0, 10)).content())
        .hasSize(1);
  }

  @Test
  void subjectsForAUserComeFromTheirDepartmentYearAndSemester() {
    User user = newUser("student");
    newSubject("Signals and Systems", "SS");
    newSubject("Digital Design", "DD");

    assertThat(
            userService
                .findSubjects(user.getId(), PageRequest.of(0, 10, Sort.by("name")))
                .content())
        .extracting("code")
        .containsExactly("DD", "SS");
  }
}
