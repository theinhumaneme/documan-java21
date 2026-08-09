// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Filter values arrive as query parameters, so this is an injection boundary. */
class SearchFilterTest {

  @Test
  void anEmptyFilterYieldsNullRatherThanAnEmptyExpression() {
    SearchFilter filter = SearchFilter.forIndex(SearchIndex.FILES);

    assertThat(filter.isEmpty()).isTrue();
    assertThat(filter.build()).isNull();
  }

  @Test
  void nullValuesAreSkippedSoOptionalParametersPassStraightThrough() {
    String expression =
        SearchFilter.forIndex(SearchIndex.FILES)
            .equals("departmentId", null)
            .equals("yearId", 2)
            .build();

    assertThat(expression).isEqualTo("yearId = 2");
  }

  @Test
  void multipleClausesAreCombinedWithAnd() {
    String expression =
        SearchFilter.forIndex(SearchIndex.FILES)
            .equals("departmentId", 1)
            .equals("yearId", 2)
            .equals("lab", true)
            .build();

    assertThat(expression).isEqualTo("departmentId = 1 AND yearId = 2 AND lab = true");
  }

  @Test
  void numbersAndBooleansAreNotQuoted() {
    assertThat(SearchFilter.forIndex(SearchIndex.FILES).equals("subjectId", 7).build())
        .isEqualTo("subjectId = 7");
    assertThat(SearchFilter.forIndex(SearchIndex.FILES).equals("theory", false).build())
        .isEqualTo("theory = false");
  }

  @Test
  void stringsAreQuoted() {
    assertThat(SearchFilter.forIndex(SearchIndex.FILES).equals("extension", "pdf").build())
        .isEqualTo("extension = \"pdf\"");
  }

  /** A value must not be able to close its quote and append its own clause. */
  @Test
  void embeddedQuotesAreEscaped() {
    String expression =
        SearchFilter.forIndex(SearchIndex.FILES)
            .equals("extension", "pdf\" OR departmentId = 1 OR extension = \"doc")
            .build();

    assertThat(expression)
        .isEqualTo("extension = \"pdf\\\" OR departmentId = 1 OR extension = \\\"doc\"");
  }

  @Test
  void backslashesAreEscapedBeforeQuotes() {
    assertThat(SearchFilter.forIndex(SearchIndex.FILES).equals("extension", "a\\b").build())
        .isEqualTo("extension = \"a\\\\b\"");
  }

  /** Guards against probing fields that were never meant to be queryable. */
  @Test
  void anAttributeThatIsNotFilterableIsRejected() {
    assertThatThrownBy(
            () -> SearchFilter.forIndex(SearchIndex.FILES).equals("objectUrl", "anything"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not filterable");
  }
}
