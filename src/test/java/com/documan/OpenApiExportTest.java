// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Writes the OpenAPI document springdoc generates from the controllers to {@code
 * target/openapi.json}.
 *
 * <p>The same arrangement as {@link SchemaExportTest}: the artifact is produced from the code
 * rather than maintained beside it, so it cannot drift. Run the suite, then copy the generated file
 * over — {@code ./extract-openapi-json.sh} does both. The committed {@code openapi.json} is in turn
 * what the frontend generates its TypeScript from, so a response type that changes here reaches the
 * client as a compile error rather than as a runtime surprise.
 *
 * <p>Two springdoc properties make the output fit to commit. Pretty-printing keeps the diff
 * readable, and ordering by keys makes it deterministic — without it the path and schema maps come
 * out in bean-registration order, which varies between runs and would produce a large meaningless
 * diff every time. They are set here rather than globally because they only matter for the exported
 * copy; the live endpoint has no reason to pay for whitespace.
 *
 * <p>Search is switched on deliberately. The whole search layer, {@code SearchController} included,
 * is gated on {@code @ConditionalOnSearchEnabled}, and the test defaults leave search off — so
 * exporting under those defaults would silently omit every {@code /api/v1/search} path and generate
 * a client with no search types, which the finder depends on. No Meilisearch is needed to do this:
 * the {@code Client} bean only holds configuration, and {@code IndexBootstrap} catches the failure
 * to reach a server and logs a warning.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(DatastoreContainers.class)
@TestPropertySource(
    properties = {
      "documan.search.enabled=true",
      "springdoc.writer-with-default-pretty-printer=true",
      "springdoc.writer-with-order-by-keys=true"
    })
class OpenApiExportTest {

  /** No bucket is reachable in a test, and generating a document never touches one. */
  @MockitoBean private S3Client s3Client;

  @Autowired private MockMvc mockMvc;

  @Test
  void everyControllerIsDocumented() throws Exception {
    String spec =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Path generated = Path.of("target", "openapi.json");
    Files.createDirectories(generated.getParent());
    Files.writeString(generated, spec.endsWith("\n") ? spec : spec + "\n", StandardCharsets.UTF_8);

    // One representative path per controller. A controller that stops being mapped — or one added
    // without being documented — fails here rather than shipping a client that cannot call it.
    assertThat(spec)
        .contains("\"/api/v1/department/all\"")
        .contains("\"/api/v1/year/all\"")
        .contains("\"/api/v1/semester/all\"")
        .contains("\"/api/v1/subject/semester\"")
        .contains("\"/api/v1/file/subject\"")
        .contains("\"/api/v1/post\"")
        .contains("\"/api/v1/comment\"")
        .contains("\"/api/v1/user\"")
        .contains("\"/api/v1/role\"")
        .contains("\"/api/v1/search/files\"");

    // The response DTOs have to appear as named schemas. This is the guard against a handler
    // regressing to ResponseEntity<?>: a wildcard has no schema, so nothing would reference the
    // DTO, and it would quietly drop out of components while every path above still passed.
    assertThat(spec)
        .contains("\"FileResponse\"")
        .contains("\"SubjectResponse\"")
        .contains("\"DepartmentResponse\"")
        .contains("\"PostResponse\"")
        .contains("\"UserResponse\"");
  }
}
