// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.request;

/**
 * What a moderator may grant or withdraw, and nothing else.
 *
 * <p>Separate from {@link UpdateUserRequest} rather than folded into it, for two reasons. The
 * console's action is "grant commenting", not "replace this profile", and sending a whole profile
 * to flip one boolean means a race in which two administrators editing different things overwrite
 * each other's field. More concretely, {@code UpdateUserRequest} requires a department, a year and
 * a semester — which an account provisioned from a token does not have, because the identity
 * provider knows who someone is and not what they are studying. Every permission change for a newly
 * provisioned user would have failed validation on three fields the caller never meant to touch.
 *
 * <p>All three are boxed and optional: null means "leave this one alone", so a caller may send one
 * without stating an opinion about the other two.
 */
public record UpdatePermissionsRequest(Boolean canPost, Boolean canComment, Boolean verified) {}
