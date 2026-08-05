// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.config;

import com.documan.dto.response.CommentResponse;
import com.documan.dto.response.DepartmentResponse;
import com.documan.dto.response.PostResponse;
import com.documan.dto.response.RoleResponse;
import com.documan.dto.response.SemesterResponse;
import com.documan.dto.response.SubjectResponse;
import com.documan.dto.response.UserResponse;
import com.documan.dto.response.YearResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Replaces the hand-rolled {@code RedisCacheService}.
 *
 * <p>Four behaviours change materially:
 *
 * <ul>
 *   <li>Entries now expire. The previous implementation set keys with no TTL and no eviction
 *       policy, so a restart was the only way to shed stale entries.
 *   <li>The application no longer deletes every key in the selected Redis database on context
 *       refresh, so the database no longer has to be exclusively owned by this service.
 *   <li>A single serializer instance per cache is shared, instead of allocating an {@code
 *       ObjectMapper} on every individual cache read and write.
 *   <li>Cached values are response records rather than JPA entities, which removes the lazy-loading
 *       and Jackson back-reference hazards of caching managed entities.
 * </ul>
 *
 * <p>Every cache holds exactly one type, so each gets a concretely typed serializer. That keeps
 * polymorphic type hints out of the payload and avoids opening a deserialization gadget surface.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  public static final String USERS = "users";
  public static final String POSTS = "posts";
  public static final String COMMENTS = "comments";
  public static final String SUBJECTS = "subjects";
  public static final String DEPARTMENTS = "departments";
  public static final String YEARS = "years";
  public static final String SEMESTERS = "semesters";
  public static final String ROLES = "roles";

  private static final Duration ENTITY_TTL = Duration.ofMinutes(10);
  private static final Duration REFERENCE_TTL = Duration.ofHours(6);

  @Bean
  RedisCacheConfiguration redisCacheConfiguration() {
    return baseConfiguration().entryTtl(ENTITY_TTL);
  }

  @Bean
  RedisCacheManagerBuilderCustomizer cacheCustomizer() {
    Map<String, RedisCacheConfiguration> caches = new LinkedHashMap<>();
    caches.put(USERS, typed(UserResponse.class, ENTITY_TTL));
    caches.put(POSTS, typed(PostResponse.class, ENTITY_TTL));
    caches.put(COMMENTS, typed(CommentResponse.class, ENTITY_TTL));
    caches.put(SUBJECTS, typed(SubjectResponse.class, REFERENCE_TTL));
    caches.put(DEPARTMENTS, typed(DepartmentResponse.class, REFERENCE_TTL));
    caches.put(YEARS, typed(YearResponse.class, REFERENCE_TTL));
    caches.put(SEMESTERS, typed(SemesterResponse.class, REFERENCE_TTL));
    caches.put(ROLES, typed(RoleResponse.class, REFERENCE_TTL));
    return builder -> builder.withInitialCacheConfigurations(caches);
  }

  private static RedisCacheConfiguration typed(Class<?> payloadType, Duration ttl) {
    return baseConfiguration()
        .entryTtl(ttl)
        .serializeValuesWith(
            SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(payloadType)));
  }

  private static RedisCacheConfiguration baseConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .disableCachingNullValues()
        .prefixCacheNameWith("documan:")
        .serializeKeysWith(SerializationPair.fromSerializer(RedisSerializer.string()));
  }
}
