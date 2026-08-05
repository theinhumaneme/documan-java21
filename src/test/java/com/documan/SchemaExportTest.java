// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Writes the PostgreSQL DDL implied by the current entity mapping to {@code
 * target/schema-postgres.sql}.
 *
 * <p>This keeps the checked-in {@code schema.sql} honest without needing a live database: run the
 * suite, then copy the generated file over. It also fails if a table the application depends on
 * stops being generated, which is the drift that left the previous snapshot stale.
 */
@SpringBootTest(
    properties = {
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
      "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/schema-postgres.sql"
    })
@Import(DatastoreContainers.class)
class SchemaExportTest {

  @MockitoBean private S3Client s3Client;

  @Test
  void everyExpectedTableIsGenerated() throws Exception {
    Path generated = Path.of("target", "schema-postgres.sql");
    assertThat(generated).exists();

    String ddl = Files.readString(generated).toLowerCase();
    assertThat(ddl)
        .contains("create table documan_user")
        .contains("create table post")
        .contains("create table comment")
        .contains("create table file")
        .contains("create table subject")
        .contains("create table role")
        .contains("create table year")
        .contains("create table semester")
        .contains("create table department")
        // The join tables that replaced the @ManyToMany collections.
        .contains("create table post_vote")
        .contains("create table comment_vote")
        .contains("create table favourite_post")
        .contains("create table favourite_file")
        .contains("create table search_outbox");

    // The @ManyToMany join tables must be gone.
    assertThat(ddl)
        .doesNotContain("create table upvoted_posts")
        .doesNotContain("create table downvoted_posts")
        .doesNotContain("create table favourite_posts")
        .doesNotContain("create table favourite_files")
        .doesNotContain("create table upvoted_comments")
        .doesNotContain("create table downvoted_comments");

    // Denormalised tallies and optimistic-locking columns.
    assertThat(ddl).contains("upvote_count").contains("downvote_count").contains("favourite_count");
  }
}
