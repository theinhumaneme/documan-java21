// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.documan.entity.*;
import com.documan.search.document.FileDocument;
import com.documan.search.document.PostDocument;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class DocumentFactoryTest {

  private final DocumentFactory factory = new DocumentFactory();

  @Test
  void extensionIsDerivedFromTheFilenameLowerCasedAndWithoutTheDot() {
    assertThat(DocumentFactory.extensionOf("Lecture Notes.PDF")).isEqualTo("pdf");
    assertThat(DocumentFactory.extensionOf("archive.tar.gz")).isEqualTo("gz");
  }

  @Test
  void aFilenameWithNoUsableExtensionYieldsEmptyRatherThanNull() {
    // Empty keeps the attribute filterable; null would need special handling in every query.
    assertThat(DocumentFactory.extensionOf("README")).isEmpty();
    assertThat(DocumentFactory.extensionOf("trailing.")).isEmpty();
    assertThat(DocumentFactory.extensionOf(null)).isEmpty();
  }

  /** post.content is unbounded TEXT; a whole batch of them could exceed the payload limit. */
  @Test
  void oversizedContentIsTruncated() {
    String huge = "x".repeat(DocumentFactory.MAX_INDEXED_CONTENT + 500);

    assertThat(DocumentFactory.truncate(huge)).hasSize(DocumentFactory.MAX_INDEXED_CONTENT);
    assertThat(DocumentFactory.truncate("short")).isEqualTo("short");
    assertThat(DocumentFactory.truncate(null)).isNull();
  }

  @Test
  void timestampsBecomeEpochSecondsSoRangeFiltersAndSortingWorkNumerically() {
    assertThat(DocumentFactory.epochSeconds(OffsetDateTime.parse("2024-10-28T00:00:00Z")))
        .isEqualTo(1730073600L);
    assertThat(DocumentFactory.epochSeconds(null)).isZero();
  }

  @Test
  void aFileDocumentDenormalisesItsSubjectAndAcademicContext() {
    FileDocument document = factory.toDocument(sampleFile());

    assertThat(document.id()).isEqualTo(11);
    assertThat(document.name()).isEqualTo("Unit 1.pdf");
    assertThat(document.extension()).isEqualTo("pdf");
    assertThat(document.subjectName()).isEqualTo("Signals and Systems");
    assertThat(document.subjectCode()).isEqualTo("SS");
    assertThat(document.departmentName()).isEqualTo("Computer Science Engineering");
    assertThat(document.yearValue()).isEqualTo("II");
    assertThat(document.semesterName()).isEqualTo("I");
    assertThat(document.theory()).isTrue();
  }

  @Test
  void aPostDocumentCarriesItsAuthorAndAPrecomputedNetScore() {
    PostDocument document = factory.toDocument(samplePost());

    assertThat(document.authorId()).isEqualTo(3);
    assertThat(document.authorUsername()).isEqualTo("author");
    assertThat(document.upvoteCount()).isEqualTo(7);
    assertThat(document.downvoteCount()).isEqualTo(2);
    // Precomputed because Meilisearch can only sort on a stored attribute.
    assertThat(document.netScore()).isEqualTo(5);
  }

  private static File sampleFile() {
    Department department = new Department();
    department.setId(1);
    department.setName("Computer Science Engineering");
    Year year = new Year();
    year.setId(2);
    year.setValue("II");
    Semester semester = new Semester();
    semester.setId(1);
    semester.setName("I");

    Subject subject = new Subject();
    subject.setId(5);
    subject.setName("Signals and Systems");
    subject.setCode("SS");
    subject.setTheory(true);
    subject.setDepartment(department);
    subject.setYear(year);
    subject.setSemester(semester);

    File file = new File();
    file.setId(11);
    file.setName("Unit 1.pdf");
    file.setObjectName("abc_unit-1.pdf");
    file.setObjectURL("http://localhost/abc_unit-1.pdf");
    file.setSize(2048L);
    file.setSubject(subject);
    file.setDateCreated(OffsetDateTime.parse("2024-10-28T00:00:00Z"));
    return file;
  }

  private static Post samplePost() {
    User author = new User();
    author.setId(3);
    author.setUsername("author");

    Post post = new Post();
    post.setId(9);
    post.setTitle("title");
    post.setDescription("description");
    post.setContent("content");
    post.setUser(author);
    post.setUpvoteCount(7);
    post.setDownvoteCount(2);
    post.setDateCreated(OffsetDateTime.parse("2024-10-28T00:00:00Z"));
    post.setDateModified(OffsetDateTime.parse("2024-10-29T00:00:00Z"));
    return post;
  }
}
