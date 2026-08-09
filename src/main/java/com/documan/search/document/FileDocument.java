// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.document;

/**
 * Subject and academic fields are denormalised because people search by subject name or department,
 * never by numeric id. That is what makes a subject rename fan out to its files.
 *
 * <p>Timestamps are epoch seconds so Meilisearch can filter ranges and sort numerically.
 */
public record FileDocument(
    Integer id,
    String name,
    String extension,
    String objectName,
    String objectUrl,
    Long size,
    long favouriteCount,
    Integer subjectId,
    String subjectName,
    String subjectCode,
    boolean lab,
    boolean theory,
    Integer departmentId,
    String departmentName,
    Integer yearId,
    String yearValue,
    Integer semesterId,
    String semesterName,
    long dateCreated) {}
