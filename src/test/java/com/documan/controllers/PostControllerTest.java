// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.documan.dto.response.PostResponse;
import com.documan.entity.VoteType;
import com.documan.exception.ResourceNotFoundException;
import com.documan.service.FavouriteService;
import com.documan.service.PostService;
import com.documan.service.VoteService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Contract-level checks: status codes, validation shape and the RFC 9457 error body. */
@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

  private static final PostResponse SAMPLE =
      new PostResponse(
          1,
          "title",
          "description",
          "content",
          0,
          0,
          0,
          7,
          "author",
          OffsetDateTime.parse("2024-10-28T00:00:00Z"),
          OffsetDateTime.parse("2024-10-28T00:00:00Z"));

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PostService postService;
  @MockitoBean private VoteService voteService;
  @MockitoBean private FavouriteService favouriteService;

  @Test
  void creatingAPostReturns201() throws Exception {
    given(postService.create(any(), eq(7))).willReturn(SAMPLE);

    mockMvc
        .perform(
            post("/api/v1/post")
                .param("userId", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"title","description":"description","content":"content"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.authorUsername").value("author"));
  }

  @Test
  void aBlankTitleIsRejectedAsAValidationProblem() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/post")
                .param("userId", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"","description":"d","content":"c"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.errors.title").exists());
  }

  @Test
  void anOverlongTitleIsRejected() throws Exception {
    String longTitle = "x".repeat(151);

    mockMvc
        .perform(
            post("/api/v1/post")
                .param("userId", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"%s\",\"description\":\"d\",\"content\":\"c\"}"
                        .formatted(longTitle)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.title").exists());
  }

  @Test
  void aMissingPostBecomesAProblemDetail404() throws Exception {
    willThrow(new ResourceNotFoundException("Post", 42)).given(postService).findById(42);

    mockMvc
        .perform(get("/api/v1/post").param("postId", "42"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Resource not found"))
        .andExpect(jsonPath("$.detail").value("Post 42 was not found"))
        .andExpect(jsonPath("$.instance").value("/api/v1/post"));
  }

  @Test
  void deletingAPostReturns204WithNoBody() throws Exception {
    mockMvc
        .perform(delete("/api/v1/post").param("postId", "1"))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
  }

  @Test
  void voteTypeIsBoundAsAnEnum() throws Exception {
    given(voteService.votePost(1, 7, VoteType.UPVOTE)).willReturn(SAMPLE);

    mockMvc
        .perform(
            post("/api/v1/post/vote")
                .param("voteType", "UPVOTE")
                .param("postId", "1")
                .param("userId", "7"))
        .andExpect(status().isOk());
  }

  /** An unknown direction is rejected at binding time rather than silently returning nothing. */
  @Test
  void anUnknownVoteTypeIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/post/vote")
                .param("voteType", "sideways")
                .param("postId", "1")
                .param("userId", "7"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listEndpointsReturnThePaginationEnvelope() throws Exception {
    given(postService.findAll(any()))
        .willReturn(
            new com.documan.dto.response.PageResponse<>(
                java.util.List.of(SAMPLE), 0, 20, 1, 1, true, true));

    mockMvc
        .perform(get("/api/v1/post/all"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.last").value(true));
  }
}
