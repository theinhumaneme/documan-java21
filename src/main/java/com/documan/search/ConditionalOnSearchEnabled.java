// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Present only when {@code documan.search.enabled} is true.
 *
 * <p>Every bean in the search layer hangs off this one switch, so that the whole feature appears
 * and disappears together.
 *
 * <p>These classes previously used {@code @ConditionalOnBean} to chain off each other — the gateway
 * on the {@code Client}, the service on the gateway, the controller on the service. That is only
 * reliable on auto-configuration classes, which are ordered after all regular bean definitions have
 * been registered. On component-scanned classes the condition is evaluated during scanning, so
 * whether it sees the bean it is asking about depends on the order the scanner happens to reach
 * them. The failure is silent and partial: with Meilisearch reachable, {@code IndexBootstrap} was
 * dropped while the beans that depend on it survived, and startup died on a missing dependency for
 * a bean that should never have been created in the first place.
 *
 * <p>A property is read from the {@code Environment}, which is fully populated before any bean
 * definition is evaluated. There is no ordering question, and one switch cannot half-apply.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(prefix = "documan.search", name = "enabled", havingValue = "true")
public @interface ConditionalOnSearchEnabled {}
