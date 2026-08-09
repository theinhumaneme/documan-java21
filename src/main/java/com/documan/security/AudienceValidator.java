// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rejects a token that was minted for a different API.
 *
 * <p>Spring's defaults check the signature, the issuer and the clock. None of them check {@code
 * aud}, so without this any service sharing the issuer would hold a key to this one: a token it
 * legitimately obtained would pass validation here.
 *
 * <p>Only installed when an audience is configured. See {@code AuthProperties} for why leaving it
 * unset is defensible while one Clerk instance serves one application.
 */
public final class AudienceValidator implements OAuth2TokenValidator<Jwt> {

  private final String audience;

  public AudienceValidator(String audience) {
    this.audience = audience;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    if (token.getAudience().contains(audience)) {
      return OAuth2TokenValidatorResult.success();
    }
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN,
            "Token audience %s is not %s".formatted(token.getAudience(), audience),
            null));
  }
}
