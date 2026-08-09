// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

/**
 * The chain every request passes through.
 *
 * <p>Identity is Clerk and the browser holds the token, so this service is a resource server: it
 * validates a bearer JWT on the way in and mints nothing. Stateless because the token carries the
 * identity, and CSRF stays disabled on the same grounds — CSRF defends a cookie session the browser
 * attaches automatically, and a bearer token in an {@code Authorization} header is not one.
 *
 * <p><b>Reading material stays open.</b> The library, the blog and search are public — a student
 * looking up a syllabus should not have to sign in, and the interface already treats an anonymous
 * reader as a first-class one. So the rule is by method rather than by path: {@code GET} is open,
 * everything that changes something needs a token. That is one line here instead of a list of paths
 * that would fall out of step with the controllers the first time one is added.
 *
 * <p><b>Reading people does not.</b> Everything under {@code /user} needs a token whatever the
 * method, because the safety of the rule above rests on GETs returning material and these return
 * the directory: names, email addresses, and each reader's own votes and favourites.
 *
 * <p><b>Who may do what is not decided here.</b> This chain answers only "is there a valid token",
 * and every rule past that lives on the controller methods as {@code @PreAuthorize}, evaluated
 * against {@link Permissions}. Two reasons it is not expressed as path matchers on this chain: the
 * interesting rules are about a <em>resource</em> rather than a URL — whether this maintainer may
 * touch this folder — which a matcher cannot see; and a list of paths here silently stops covering
 * a controller the moment someone adds a method to it, whereas an annotation is next to the thing
 * it protects.
 *
 * <p>The {@code ?userId=} parameter is still on most write endpoints and still lets a caller name
 * somebody else as the actor. That is a separate defect and is called out in {@link CurrentUser}.
 * It does not weaken what is enforced here, because every rule below is asked about the token's
 * owner, never about the named id — a reader cannot promote themselves by naming an administrator.
 * What it still allows is writing <em>as</em> another user once you are permitted to write at all.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class WebSecurityConfig {

  /**
   * Read without authentication. Health backs container probes, and the OpenAPI document is what
   * the TypeScript client is generated from — both are consumed by things that have no token and no
   * way to get one.
   */
  private static final String[] PUBLIC_PATHS = {
    "/actuator/health",
    "/actuator/health/**",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html"
  };

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http, NimbusJwtDecoder jwtDecoder, AuthProperties auth) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(PUBLIC_PATHS)
                    .permitAll()
                    // Preflight carries no Authorization header by definition; rejecting it would
                    // fail the request the browser has not made yet.
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    // The exception to the rule below. Reading *material* is
                    // public; reading *people* is not. These return names, email
                    // addresses and the reader's own votes and favourites, and
                    // "it is a GET" is not a reason to hand that to anyone who
                    // asks — /user/all would have served the whole directory.
                    //
                    // OpenApiConfig.closedByChain mirrors this rule so the
                    // generated spec does not describe these as public. Change
                    // the matcher and that has to change with it.
                    .requestMatchers("/api/v1/user/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
  }

  /**
   * Built by hand for two reasons.
   *
   * <p>The first is {@link AudienceValidator}: the default validators check the signature, the
   * issuer and the clock, and none of them check who the token was minted for.
   *
   * <p>The second is that this takes the signing keys directly rather than discovering them.
   * Configuring the issuer alone is the more usual form, but it makes Spring fetch Clerk's {@code
   * .well-known/openid-configuration} while the context is starting, and a slow or unreachable
   * Clerk then means the service does not come up at all — a sign-in outage escalated into a total
   * one, taking the library and the blog down with it, neither of which needs a token. Naming the
   * key set skips discovery, so the first fetch happens on the first token and a failure there
   * costs one request instead of the process. The issuer is still checked; it moves from being the
   * thing looked up to being the thing asserted.
   */
  @Bean
  NimbusJwtDecoder jwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
      AuthProperties auth) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuer);
    if (StringUtils.hasText(auth.audience())) {
      validator =
          new DelegatingOAuth2TokenValidator<>(validator, new AudienceValidator(auth.audience()));
    }
    decoder.setJwtValidator(validator);
    return decoder;
  }
}
