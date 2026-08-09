// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.mapper;

import com.documan.dto.response.FileResponse;
import com.documan.entity.File;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface FileMapper {

  @Mapping(target = "subjectId", source = "subject.id")
  /* Null when the file sits at the root of its subject; MapStruct guards the navigation. */
  @Mapping(target = "folderId", source = "folder.id")
  FileResponse toResponse(File file);
}
