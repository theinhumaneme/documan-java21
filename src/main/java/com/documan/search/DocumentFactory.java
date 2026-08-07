// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import com.documan.entity.File;
import com.documan.entity.Subject;
import com.documan.search.document.FileDocument;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Entity to document conversion.
 *
 * <p>Hand-written rather than generated: the build runs MapStruct with {@code
 * unmappedTargetPolicy=ERROR}, and the epoch-second conversions, extension derivation and content
 * truncation would fight the generator more than they would benefit from it.
 */
@Component
public class DocumentFactory {

  public FileDocument toDocument(File file) {
    Subject subject = file.getSubject();
    return new FileDocument(
        file.getId(),
        file.getName(),
        extensionOf(file.getName()),
        file.getObjectName(),
        file.getObjectURL(),
        file.getSize(),
        file.getFavouriteCount(),
        subject.getId(),
        subject.getName(),
        subject.getCode(),
        subject.isLab(),
        subject.isTheory(),
        subject.getDepartment().getId(),
        subject.getDepartment().getName(),
        subject.getYear().getId(),
        subject.getYear().getValue(),
        subject.getSemester().getId(),
        subject.getSemester().getName(),
        epochSeconds(file.getDateCreated()));
  }

  /** Lower-cased and dot-free so it can be used as an exact filter value. */
  static String extensionOf(String filename) {
    if (filename == null) {
      return "";
    }
    int dot = filename.lastIndexOf('.');
    if (dot < 0 || dot == filename.length() - 1) {
      return "";
    }
    return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  static long epochSeconds(OffsetDateTime timestamp) {
    return timestamp == null ? 0L : timestamp.toEpochSecond();
  }
}
