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
  FileResponse toResponse(File file);
}
