// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import com.documan.entity.Comment;
import com.documan.entity.File;
import com.documan.entity.Post;
import com.documan.entity.Subject;
import com.documan.search.document.CommentDocument;
import com.documan.search.document.FileDocument;
import com.documan.search.document.PostDocument;
import com.documan.search.document.SubjectDocument;
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

  /**
   * {@code post.content} is unbounded TEXT behind a 25 MB upload limit. Indexing it whole would
   * risk exceeding Meilisearch's payload limit on a batch; the full body is served from PostgreSQL
   * anyway, so only enough to match on is indexed.
   */
  static final int MAX_INDEXED_CONTENT = 32_000;

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

  public SubjectDocument toDocument(Subject subject) {
    return new SubjectDocument(
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
        subject.getSemester().getName());
  }

  public PostDocument toDocument(Post post) {
    return new PostDocument(
        post.getId(),
        post.getTitle(),
        post.getDescription(),
        truncate(post.getContent()),
        post.getUser().getId(),
        post.getUser().getUsername(),
        post.getUpvoteCount(),
        post.getDownvoteCount(),
        post.getFavouriteCount(),
        post.getUpvoteCount() - post.getDownvoteCount(),
        epochSeconds(post.getDateCreated()),
        epochSeconds(post.getDateModified()));
  }

  public CommentDocument toDocument(Comment comment) {
    return new CommentDocument(
        comment.getId(),
        truncate(comment.getContent()),
        comment.getPost().getId(),
        comment.getUser().getId(),
        comment.getUser().getUsername(),
        comment.getUpvoteCount(),
        comment.getDownvoteCount(),
        epochSeconds(comment.getDateCreated()));
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

  static String truncate(String value) {
    if (value == null || value.length() <= MAX_INDEXED_CONTENT) {
      return value;
    }
    return value.substring(0, MAX_INDEXED_CONTENT);
  }

  static long epochSeconds(OffsetDateTime timestamp) {
    return timestamp == null ? 0L : timestamp.toEpochSecond();
  }
}
