// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One PostgreSQL and one Redis for the whole suite.
 *
 * <p>Declared as singleton beans in an imported configuration rather than per-class
 * {@code @Container} fields: Spring reuses the application context across test classes, so the
 * containers start once instead of once per class. With H2 gone every database test runs here, and
 * restarting containers per class would dominate the suite's runtime.
 *
 * <p>PostgreSQL specifically, not a substitute — the search outbox relies on {@code ON CONFLICT},
 * {@code LEAST} and identity columns, and the counter updates rely on PostgreSQL locking.
 */
@TestConfiguration(proxyBeanMethods = false)
public class DatastoreContainers {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgres() {
    return new PostgreSQLContainer("postgres:17-alpine").withReuse(true);
  }

  @Bean
  @ServiceConnection
  RedisContainer redis() {
    return new RedisContainer("redis:7.4-alpine").withReuse(true);
  }
}
