// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * @param defaultFolders which of the {@code DefaultFolder} slugs to create the subject with —
 *     {@code coursefiles}, {@code unit-1} … {@code unit-5}, {@code lab}. Omitted or empty means the
 *     subject starts with no folders at all, which is a legitimate choice: files can be uploaded to
 *     the subject's root and filed later. {@code lab} is ignored for a subject that has no lab, and
 *     an unrecognised slug is rejected rather than skipped. Absent from the JSON behaves the same
 *     as empty, so a caller written against the previous shape of this request still works.
 */
public record CreateSubjectRequest(
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Size(max = 20) String code,
    boolean lab,
    boolean theory,
    @NotNull Integer departmentId,
    @NotNull Integer yearId,
    @NotNull Integer semesterId,
    Set<String> defaultFolders) {}
