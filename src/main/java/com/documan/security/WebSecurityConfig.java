// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The chain every request passes through. Open, for now.
 *
 * <p>Nothing here authenticates anyone yet: the API is read-mostly and the identity story is Azure
 * AD, which has not landed. This exists so that when it does, there is one place to add it — the
 * chain, the session policy and the CSRF decision are already stated rather than left to Spring
 * Boot's defaults, and a bearer-token filter slots in without rearranging anything.
 *
 * <p>Stateless because the token will carry the identity. CSRF is disabled on the same grounds: it
 * defends a cookie session, and there is not one to defend.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authorize -> authorize.requestMatchers("/**").permitAll())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
  }
}
