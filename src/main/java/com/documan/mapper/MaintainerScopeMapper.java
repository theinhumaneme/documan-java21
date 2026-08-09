// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.mapper;

import com.documan.dto.response.MaintainerScopeResponse;
import com.documan.entity.MaintainerScope;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = ReferenceMapper.class)
public interface MaintainerScopeMapper {

  @Mapping(target = "userId", source = "user.id")
  MaintainerScopeResponse toResponse(MaintainerScope scope);
}
