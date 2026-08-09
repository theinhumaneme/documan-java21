// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dao;

import com.documan.entity.Folder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderDao extends JpaRepository<Folder, Integer> {

  /**
   * Every folder of one subject. Ordered by id only so the result is stable; the display order —
   * coursefiles, then units ascending, then lab, then custom folders — is applied by the client,
   * because it is a presentation rule and a stored {@code position} would have to be maintained on
   * every insert to express something the slug already implies.
   */
  List<Folder> findBySubjectIdOrderByIdAsc(Integer subjectId);

  Optional<Folder> findBySubjectIdAndSlug(Integer subjectId, String slug);

  boolean existsBySubjectIdAndSlug(Integer subjectId, String slug);

  /**
   * Used when a subject is deleted. Subject deletion has no JPA cascade — only {@code Post}
   * declares one — so a subject's folders have to be cleared explicitly or the foreign key blocks
   * the delete.
   */
  void deleteBySubjectId(Integer subjectId);
}
