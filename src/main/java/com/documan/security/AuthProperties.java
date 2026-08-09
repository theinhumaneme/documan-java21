// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * What this service accepts a token for. The issuer lives under {@code
 * spring.security.oauth2.resourceserver} because Spring reads it directly; this one is ours.
 *
 * @param audience who the token must have been minted for, or blank to accept any audience from the
 *     configured issuer. Blank is not a hole: the issuer is a single Clerk instance belonging to
 *     this application, so there is no other API in it whose tokens could be replayed here. It
 *     becomes worth setting the day a second service shares the instance, and then it has to match
 *     the {@code aud} the JWT template stamps, exactly.
 */
@ConfigurationProperties(prefix = "documan.auth")
public record AuthProperties(@DefaultValue("") String audience) {}
