// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.json.JacksonJsonHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "documan.search", name = "enabled", havingValue = "true")
public class MeilisearchConfig {

  /**
   * The SDK defaults to Gson, which cannot serialise {@code java.time} types — and every document
   * carries timestamps. Handing it a Jackson handler keeps one JSON library in charge of our data.
   */
  @Bean
  Client meilisearchClient(SearchProperties properties) {
    return new Client(new Config(properties.host(), properties.apiKey(), new JacksonJsonHandler()));
  }
}
