// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.mapper;

import com.documan.dto.response.FolderResponse;
import com.documan.entity.Folder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface FolderMapper {

  /**
   * The count is passed in rather than read off the entity. A folder has no files collection —
   * adding one so this mapping could call {@code size()} would load every file of every folder to
   * render a listing — so the service fetches all the counts for a subject in one grouped query and
   * hands the right one in.
   */
  @Mapping(target = "subjectId", source = "folder.subject.id")
  @Mapping(target = "isDefault", source = "folder.defaultFolder")
  @Mapping(target = "fileCount", source = "fileCount")
  FolderResponse toResponse(Folder folder, long fileCount);
}
