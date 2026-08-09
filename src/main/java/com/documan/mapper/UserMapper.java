// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.mapper;

import com.documan.dto.response.UserResponse;
import com.documan.entity.User;
import org.mapstruct.Mapper;

@Mapper(uses = ReferenceMapper.class)
public interface UserMapper {

  UserResponse toResponse(User user);
}
