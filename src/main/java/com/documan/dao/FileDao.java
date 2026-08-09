// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dao;

import com.documan.entity.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileDao extends JpaRepository<File, Integer> {

  Page<File> findBySubjectId(Integer subjectId, Pageable pageable);

  /** Files filed under one folder. */
  Page<File> findByFolderId(Integer folderId, Pageable pageable);

  /** Whether a folder still holds anything. A non-empty folder is refused for deletion. */
  boolean existsByFolderId(Integer folderId);

  /**
   * File counts per folder for one subject, so a folder listing costs one extra query rather than
   * one per folder. Folders holding nothing are absent from the result rather than reported as
   * zero, because a group-by cannot produce a row for a folder no file references.
   */
  @Query(
      """
      select f.folder.id as folderId, count(f) as total
      from File f
      where f.subject.id = :subjectId
      group by f.folder.id
      """)
  List<FolderFileCount> countByFolderForSubject(@Param("subjectId") Integer subjectId);

  /** Projection for {@link #countByFolderForSubject}. */
  interface FolderFileCount {
    Integer getFolderId();

    long getTotal();
  }

  Optional<File> findByObjectName(String objectName);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("update File f set f.favouriteCount = f.favouriteCount + :delta where f.id = :fileId")
  void applyFavouriteDelta(@Param("fileId") Integer fileId, @Param("delta") long delta);

  /**
   * Batch load for indexing. The subject's department, year and semester are all denormalised into
   * the file document, and they are EAGER associations, so without this graph a batch of N files
   * costs 1 + 3N selects.
   */
  @EntityGraph(
      attributePaths = {"subject", "subject.department", "subject.year", "subject.semester"})
  List<File> findForIndexingByIdIn(Collection<Integer> ids);
}
