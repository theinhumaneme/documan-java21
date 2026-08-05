// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.mapper;

import com.documan.dto.response.CommentResponse;
import com.documan.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CommentMapper {

  @Mapping(target = "postId", source = "post.id")
  @Mapping(target = "authorId", source = "user.id")
  @Mapping(target = "authorUsername", source = "user.username")
  CommentResponse toResponse(Comment comment);
}
