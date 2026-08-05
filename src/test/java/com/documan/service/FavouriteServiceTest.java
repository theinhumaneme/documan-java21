// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.documan.AbstractDataTest;
import com.documan.dto.response.FileResponse;
import com.documan.dto.response.PostResponse;
import com.documan.entity.File;
import com.documan.entity.Post;
import com.documan.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FavouriteServiceTest extends AbstractDataTest {

  @Autowired private FavouriteService favouriteService;

  @Test
  void favouritingIsIdempotent() {
    User user = newUser("reader");
    Post post = newPost(newUser("author"), "title");

    favouriteService.favouritePost(post.getId(), user.getId());
    PostResponse response = favouriteService.favouritePost(post.getId(), user.getId());

    assertThat(response.favouriteCount()).isEqualTo(1);
    assertThat(postFavouriteDao.count()).isEqualTo(1);
  }

  @Test
  void removingAFavouriteDecrementsTheTally() {
    User user = newUser("reader");
    Post post = newPost(newUser("author"), "title");

    favouriteService.favouritePost(post.getId(), user.getId());
    PostResponse response = favouriteService.removeFavouritePost(post.getId(), user.getId());

    assertThat(response.favouriteCount()).isZero();
    assertThat(postFavouriteDao.count()).isZero();
  }

  @Test
  void removingAFavouriteThatWasNeverSetChangesNothing() {
    User user = newUser("reader");
    Post post = newPost(newUser("author"), "title");

    PostResponse response = favouriteService.removeFavouritePost(post.getId(), user.getId());

    assertThat(response.favouriteCount()).isZero();
  }

  /** File favourites had a join table and a model but no reachable code path before. */
  @Test
  void fileFavouritesAreTracked() {
    User user = newUser("reader");
    File file = newFile(newSubject("Signals and Systems", "SS"), "notes.pdf");

    FileResponse favourited = favouriteService.favouriteFile(file.getId(), user.getId());
    assertThat(favourited.favouriteCount()).isEqualTo(1);

    FileResponse removed = favouriteService.removeFavouriteFile(file.getId(), user.getId());
    assertThat(removed.favouriteCount()).isZero();
    assertThat(fileFavouriteDao.count()).isZero();
  }
}
