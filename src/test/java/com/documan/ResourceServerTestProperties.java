// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.TestPropertySource;

/**
 * The issuer and key set a full application context needs in order to start.
 *
 * <p>{@code WebSecurityConfig.jwtDecoder} reads both without a default, so a test that loads the
 * whole context fails on an unresolved placeholder before a single assertion runs. That is what
 * happened the moment this service became a resource server: every {@code @SpringBootTest} broke at
 * once, {@code OpenApiExportTest} among them — which is the test that generates {@code openapi.json}
 * for CI to diff, so the contract check went quiet at the same time.
 *
 * <p><b>Why an annotation rather than {@code src/test/resources/application.yml}.</b> Putting these
 * there fixes the full-context tests and breaks the sliced ones. A property under {@code
 * spring.security.oauth2.resourceserver.jwt} is what activates Spring Boot's resource-server
 * auto-configuration, and inside a {@code @WebMvcTest} that auto-configuration tries to build a
 * filter chain from an {@code HttpSecurity} bean the slice does not have. Applied here it reaches
 * exactly the tests that want a resource server and none of the ones that do not.
 *
 * <p>The host does not resolve and does not need to. {@code NimbusJwtDecoder} fetches the key set
 * lazily, on the first token it is asked to verify, and no test presents one — a test that wanted to
 * would need to sign a token and therefore bring its own key pair. {@code .invalid} is reserved by
 * RFC 2606 so it can never accidentally be somebody's real host.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@TestPropertySource(
    properties = {
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://clerk.test.invalid",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://clerk.test.invalid/.well-known/jwks.json"
    })
public @interface ResourceServerTestProperties {}
