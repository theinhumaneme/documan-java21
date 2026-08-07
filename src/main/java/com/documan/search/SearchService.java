// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import com.documan.search.document.CommentDocument;
import com.documan.search.document.FileDocument;
import com.documan.search.document.PostDocument;
import com.documan.search.document.SubjectDocument;
import com.meilisearch.sdk.SearchRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.TypeFactory;

/** Query side. Turns request parameters into a Meilisearch query and back into typed documents. */
@Service
@ConditionalOnSearchEnabled
public class SearchService {

  /** Meilisearch's own ceiling on a single page; also protects against a hostile size parameter. */
  private static final int MAX_SIZE = 100;

  private final MeilisearchGateway gateway;
  private final ObjectMapper objectMapper;

  public SearchService(MeilisearchGateway gateway, ObjectMapper objectMapper) {
    this.gateway = gateway;
    this.objectMapper = objectMapper;
  }

  public SearchResponse<FileDocument> searchFiles(
      String query,
      Integer subjectId,
      Integer departmentId,
      Integer yearId,
      Integer semesterId,
      String extension,
      Boolean lab,
      Boolean theory,
      int page,
      int size,
      String sort) {
    SearchFilter filter =
        SearchFilter.forIndex(SearchIndex.FILES)
            .equals("subjectId", subjectId)
            .equals("departmentId", departmentId)
            .equals("yearId", yearId)
            .equals("semesterId", semesterId)
            .equals("extension", extension)
            .equals("lab", lab)
            .equals("theory", theory);
    return execute(SearchIndex.FILES, query, filter, page, size, sort, FileDocument.class);
  }

  public SearchResponse<SubjectDocument> searchSubjects(
      String query,
      Integer departmentId,
      Integer yearId,
      Integer semesterId,
      Boolean lab,
      Boolean theory,
      int page,
      int size,
      String sort) {
    SearchFilter filter =
        SearchFilter.forIndex(SearchIndex.SUBJECTS)
            .equals("departmentId", departmentId)
            .equals("yearId", yearId)
            .equals("semesterId", semesterId)
            .equals("lab", lab)
            .equals("theory", theory);
    return execute(SearchIndex.SUBJECTS, query, filter, page, size, sort, SubjectDocument.class);
  }

  public SearchResponse<PostDocument> searchPosts(
      String query, Integer authorId, int page, int size, String sort) {
    SearchFilter filter = SearchFilter.forIndex(SearchIndex.POSTS).equals("authorId", authorId);
    return execute(SearchIndex.POSTS, query, filter, page, size, sort, PostDocument.class);
  }

  public SearchResponse<CommentDocument> searchComments(
      String query, Integer postId, Integer authorId, int page, int size, String sort) {
    SearchFilter filter =
        SearchFilter.forIndex(SearchIndex.COMMENTS)
            .equals("postId", postId)
            .equals("authorId", authorId);
    return execute(SearchIndex.COMMENTS, query, filter, page, size, sort, CommentDocument.class);
  }

  private <T> SearchResponse<T> execute(
      SearchIndex index,
      String query,
      SearchFilter filter,
      int page,
      int size,
      String sort,
      Class<T> documentType) {

    int limit = Math.clamp(size, 1, MAX_SIZE);
    int offset = Math.max(page, 0) * limit;

    SearchRequest request =
        SearchRequest.builder()
            .q(query == null ? "" : query)
            .offset(offset)
            .limit(limit)
            .filter(filter.isEmpty() ? null : new String[] {filter.build()})
            .sort(sort == null || sort.isBlank() ? null : new String[] {sort})
            .build();

    String raw;
    try {
      raw = gateway.rawSearch(index, request);
    } catch (SearchTransportException e) {
      throw new SearchUnavailableException("Search is temporarily unavailable", 30, e);
    } catch (SearchDocumentException e) {
      // A rejected query is the caller's problem, not an outage.
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    RawResult<T> parsed =
        objectMapper.readValue(
            raw,
            TypeFactory.createDefaultInstance()
                .constructParametricType(RawResult.class, documentType));

    return new SearchResponse<>(
        parsed.hits() == null ? List.of() : parsed.hits(),
        query == null ? "" : query,
        page,
        limit,
        parsed.estimatedTotalHits(),
        parsed.processingTimeMs());
  }

  /** The subset of Meilisearch's search response this API exposes. */
  record RawResult<T>(List<T> hits, long estimatedTotalHits, long processingTimeMs) {}
}
