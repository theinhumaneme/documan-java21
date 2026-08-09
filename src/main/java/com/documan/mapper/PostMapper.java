// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.mapper;

import com.documan.dto.response.PostResponse;
import com.documan.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface PostMapper {

  @Mapping(target = "authorId", source = "user.id")
  @Mapping(target = "authorUsername", source = "user.username")
  PostResponse toResponse(Post post);
}
