// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.search.SearchResponse;
import com.documan.search.SearchService;
import com.documan.search.document.CommentDocument;
import com.documan.search.document.FileDocument;
import com.documan.search.document.PostDocument;
import com.documan.search.document.SubjectDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Full-text search.
 *
 * <p>Present only when {@code documan.search.enabled} is true, so an instance running without
 * Meilisearch does not advertise endpoints it cannot serve.
 */
@RestController
@RequestMapping("/api/v1/search")
@ConditionalOnBean(SearchService.class)
public class SearchController {

  private final SearchService searchService;

  public SearchController(SearchService searchService) {
    this.searchService = searchService;
  }

  @GetMapping("/files")
  public SearchResponse<FileDocument> files(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "subjectId", required = false) Integer subjectId,
      @RequestParam(value = "departmentId", required = false) Integer departmentId,
      @RequestParam(value = "yearId", required = false) Integer yearId,
      @RequestParam(value = "semesterId", required = false) Integer semesterId,
      @RequestParam(value = "extension", required = false) String extension,
      @RequestParam(value = "lab", required = false) Boolean lab,
      @RequestParam(value = "theory", required = false) Boolean theory,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", required = false) String sort) {
    return searchService.searchFiles(
        query,
        subjectId,
        departmentId,
        yearId,
        semesterId,
        extension,
        lab,
        theory,
        page,
        size,
        sort);
  }

  @GetMapping("/subjects")
  public SearchResponse<SubjectDocument> subjects(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "departmentId", required = false) Integer departmentId,
      @RequestParam(value = "yearId", required = false) Integer yearId,
      @RequestParam(value = "semesterId", required = false) Integer semesterId,
      @RequestParam(value = "lab", required = false) Boolean lab,
      @RequestParam(value = "theory", required = false) Boolean theory,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", required = false) String sort) {
    return searchService.searchSubjects(
        query, departmentId, yearId, semesterId, lab, theory, page, size, sort);
  }

  @GetMapping("/posts")
  public SearchResponse<PostDocument> posts(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "authorId", required = false) Integer authorId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", required = false) String sort) {
    return searchService.searchPosts(query, authorId, page, size, sort);
  }

  @GetMapping("/comments")
  public SearchResponse<CommentDocument> comments(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "postId", required = false) Integer postId,
      @RequestParam(value = "authorId", required = false) Integer authorId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", required = false) String sort) {
    return searchService.searchComments(query, postId, authorId, page, size, sort);
  }
}
