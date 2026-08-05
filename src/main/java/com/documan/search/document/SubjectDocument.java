// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.document;

public record SubjectDocument(
    Integer id,
    String name,
    String code,
    boolean lab,
    boolean theory,
    Integer departmentId,
    String departmentName,
    Integer yearId,
    String yearValue,
    Integer semesterId,
    String semesterName) {}
