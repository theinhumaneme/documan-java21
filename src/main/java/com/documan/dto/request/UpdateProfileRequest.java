// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Where the reader sits in the course structure, and whether they accepted the terms.
 *
 * <p>What the identity provider cannot answer. Clerk proves who someone is and that their address
 * is real; it has no idea which department they study in. So these are asked on the registration
 * form and sent here once a session exists, which is the only moment both halves are known.
 *
 * <p>Separate from {@link UpdateUserRequest} because that one also carries the username, the email
 * and the names — fields the identity provider owns now, and which a reader editing their own
 * timetable has no business restating. Sending them back would be inviting a client to overwrite
 * the identity it was issued.
 *
 * <p>All three references are required. A half-filed reader is the state this exists to leave, and
 * accepting one of the three would mean the library still could not scope to them.
 */
public record UpdateProfileRequest(
    @NotNull Integer departmentId,
    @NotNull Integer yearId,
    @NotNull Integer semesterId,
    boolean acceptedTermsOfService) {}
