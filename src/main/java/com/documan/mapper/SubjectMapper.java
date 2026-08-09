// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.mapper;

import com.documan.dto.response.SubjectResponse;
import com.documan.entity.Subject;
import org.mapstruct.Mapper;

@Mapper(uses = ReferenceMapper.class)
public interface SubjectMapper {

  SubjectResponse toResponse(Subject subject);
}
