// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds a Meilisearch filter expression.
 *
 * <p>Two safeguards, both because filter values arrive as query parameters. Attribute names are
 * checked against the index's declared filterable set, so a caller cannot filter on — or probe for
 * — a field that was never meant to be queryable. String values are quoted and escaped, so a value
 * containing a quote or an {@code OR} cannot break out and rewrite the expression.
 */
public final class SearchFilter {

  private final Set<String> allowedAttributes;
  private final List<String> clauses = new ArrayList<>();

  private SearchFilter(SearchIndex index) {
    this.allowedAttributes = new LinkedHashSet<>(List.of(index.filterableAttributes()));
  }

  public static SearchFilter forIndex(SearchIndex index) {
    return new SearchFilter(index);
  }

  /** Ignores null values, so optional request parameters can be passed through directly. */
  public SearchFilter equals(String attribute, Object value) {
    if (value == null) {
      return this;
    }
    requireFilterable(attribute);
    clauses.add(attribute + " = " + literal(value));
    return this;
  }

  public boolean isEmpty() {
    return clauses.isEmpty();
  }

  /**
   * @return the expression, or null when no clauses were added — Meilisearch expects null, not ""
   */
  public String build() {
    return clauses.isEmpty() ? null : String.join(" AND ", clauses);
  }

  private void requireFilterable(String attribute) {
    if (!allowedAttributes.contains(attribute)) {
      throw new IllegalArgumentException(
          "'%s' is not filterable; allowed: %s".formatted(attribute, allowedAttributes));
    }
  }

  private static String literal(Object value) {
    if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    }
    return "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
