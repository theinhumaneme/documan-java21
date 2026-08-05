// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

/**
 * The four indexed aggregates.
 *
 * <p>Attribute lists are ordered and fixed. Meilisearch reindexes an entire index whenever settings
 * actually change, so these must serialise identically on every boot — building them from an
 * unordered collection would trigger a full reindex on each deploy.
 *
 * <p>Timestamps are indexed as epoch seconds rather than ISO strings so that range filters and
 * sorting work numerically.
 */
public enum SearchIndex {
  FILES(
      "files",
      new String[] {"name", "extension", "subjectName", "subjectCode"},
      new String[] {
        "extension", "subjectId", "departmentId", "yearId", "semesterId", "lab", "theory"
      },
      new String[] {"name", "size", "favouriteCount", "dateCreated"}),

  SUBJECTS(
      "subjects",
      new String[] {"name", "code"},
      new String[] {"departmentId", "yearId", "semesterId", "lab", "theory"},
      new String[] {"name", "code"}),

  POSTS(
      "posts",
      new String[] {"title", "description", "content", "authorUsername"},
      new String[] {"authorId", "authorUsername"},
      new String[] {"dateCreated", "upvoteCount", "netScore"}),

  COMMENTS(
      "comments",
      new String[] {"content", "authorUsername"},
      new String[] {"postId", "authorId"},
      new String[] {"dateCreated", "upvoteCount"});

  private final String suffix;
  private final String[] searchable;
  private final String[] filterable;
  private final String[] sortable;

  SearchIndex(String suffix, String[] searchable, String[] filterable, String[] sortable) {
    this.suffix = suffix;
    this.searchable = searchable;
    this.filterable = filterable;
    this.sortable = sortable;
  }

  public String uid(String prefix) {
    return prefix + suffix;
  }

  public String[] searchableAttributes() {
    return searchable.clone();
  }

  public String[] filterableAttributes() {
    return filterable.clone();
  }

  public String[] sortableAttributes() {
    return sortable.clone();
  }

  public static SearchIndex of(AggregateType type) {
    return switch (type) {
      case FILE -> FILES;
      case SUBJECT -> SUBJECTS;
      case POST -> POSTS;
      case COMMENT -> COMMENTS;
    };
  }
}
