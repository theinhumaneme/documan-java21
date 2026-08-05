// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dao;

import com.documan.entity.File;
import com.documan.entity.FileFavourite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FileFavouriteDao extends JpaRepository<FileFavourite, Long> {

  Optional<FileFavourite> findByFileIdAndUserId(Integer fileId, Integer userId);

  boolean existsByFileIdAndUserId(Integer fileId, Integer userId);

  @Query(
      value = "select ff.file from FileFavourite ff where ff.user.id = :userId",
      countQuery = "select count(ff) from FileFavourite ff where ff.user.id = :userId")
  Page<File> findFavouriteFilesByUser(Integer userId, Pageable pageable);

  /** Used to withdraw a departing user's contributions before the database cascade. */
  List<FileFavourite> findByUserId(Integer userId);
}
