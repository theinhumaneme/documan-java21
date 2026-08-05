// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.config;

import com.documan.search.SearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
@EnableConfigurationProperties(SearchProperties.class)
public class WebConfig {

  /**
   * Adds {@code ETag} to API GET responses and answers a matching {@code If-None-Match} with 304.
   * Reference data and file listings are read far more often than they change, so this removes a
   * large share of response bytes for repeat readers.
   *
   * <p>The filter buffers the response to hash it, so it is scoped to the API paths rather than
   * applied globally.
   */
  @Bean
  FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
    FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
        new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
    registration.addUrlPatterns("/api/*");
    registration.setName("etagFilter");
    registration.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
    return registration;
  }
}
