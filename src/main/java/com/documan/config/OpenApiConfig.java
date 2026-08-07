// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Document-level metadata for the generated OpenAPI spec.
 *
 * <p>Only the parts springdoc cannot infer live here: a title, a version, a licence and the servers
 * the document is valid against. Paths, parameters and every request and response schema are read
 * off the controller signatures at runtime, which is the whole point — a spec assembled by hand
 * drifts the first time someone changes a return type and forgets to update it.
 *
 * <p>That guarantee is only as good as the signatures. A handler returning {@code
 * ResponseEntity<?>} documents nothing, because a wildcard has no schema, and springdoc will emit
 * an empty response body for it without complaining. The concrete DTO return types on the
 * controllers are therefore load bearing: they <em>are</em> the documentation, and both the
 * committed {@code openapi.json} and the TypeScript the frontend compiles against inherit whatever
 * accuracy they have.
 *
 * <p>The version is the API's contract version — the {@code v1} in {@code /api/v1} — not the Maven
 * artifact version. A patch release does not change the contract, so tying the two together would
 * churn the committed spec on every build for no reader benefit.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI documanOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Documan API")
                .description(
                    """
                    Departments, years, semesters, subjects and the files filed under them, \
                    plus the student blog.

                    Two conventions run through everything. Identifiers are query parameters \
                    rather than path segments — `?subjectId=12`, not `/subject/12`. And \
                    collection endpoints return a `PageResponse` envelope, while the three \
                    reference lookups (`/department/all`, `/year/all`, `/semester/all`) return \
                    plain arrays, because a fixed handful of rows is not worth paging.

                    Errors are RFC 9457 problem documents, produced centrally by \
                    `GlobalExceptionHandler`.

                    This document is generated from the controller signatures and is not edited \
                    by hand. `./extract-openapi-json.sh` regenerates the committed copy.\
                    """)
                .version("v1")
                .license(
                    new License()
                        .name("MIT")
                        .url("https://github.com/theinhumaneme/documan-java21/blob/main/LICENSE")))
        .servers(
            List.of(
                new Server().url("/").description("Same origin as the caller"),
                new Server().url("http://localhost:8080").description("Local development")));
  }
}
