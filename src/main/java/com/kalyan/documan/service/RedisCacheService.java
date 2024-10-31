// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheService {

  private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

  private final RedisTemplate<String, Object> redisTemplate;

  @Autowired
  public RedisCacheService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
  }

  @EventListener(ContextRefreshedEvent.class)
  public void clearCacheOnStartup() {
    log.info("Clearing Redis cache on application startup...");
    try {
      Set<String> keys = redisTemplate.keys("*");
      if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
        log.info("Redis cache cleared.");
      }
    } catch (Exception e) {
      log.error("Error clearing Redis cache on startup", e);
    }
  }

  public <T> Optional<T> getValue(String redisKey, Class<T> objClass) {
    Object object = redisTemplate.opsForValue().get(redisKey);

    if (object != null) {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        T value = objectMapper.readValue(object.toString(), objClass);
        return Optional.of(value);
      } catch (Exception e) {
        log.error(e.toString());
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }
  }

  public <T> Optional<T> setValue(String redisKey, T value) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      String jsonValue = objectMapper.writeValueAsString(value);
      redisTemplate.opsForValue().set(redisKey, jsonValue);
      return Optional.of(value);
    } catch (Exception e) {
      log.error(e.toString());
      return Optional.empty();
    }
  }

  public void deleteValue(String redisKey) {
    redisTemplate.delete(redisKey);
  }

  public <T> Optional<T> updateValue(String redisKey, T value) {
    // delete the key first to avoid stale data state
    this.deleteValue(redisKey);
    return this.setValue(redisKey, value);
  }
}
