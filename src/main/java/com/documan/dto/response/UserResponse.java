// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.response;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Serializable so it can be written straight into the Redis cache. Note the absence of a password
 * field: the entity was previously serialised directly and relied on a Jackson annotation to keep
 * the credential out of responses.
 */
public record UserResponse(
    Integer id,
    String username,
    String firstName,
    String lastName,
    String email,
    boolean acceptedTermsOfService,
    boolean verified,
    boolean canPost,
    boolean canComment,
    RoleResponse role,
    DepartmentResponse department,
    YearResponse year,
    SemesterResponse semester,
    OffsetDateTime dateCreated,
    OffsetDateTime dateLastInteracted)
    implements Serializable {}
