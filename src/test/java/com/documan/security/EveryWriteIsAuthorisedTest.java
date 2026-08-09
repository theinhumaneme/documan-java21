// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Every endpoint that changes something carries an authorisation rule.
 *
 * <p>This is a structural test on purpose. The behavioural ones need a database and a token, so they
 * are the slowest and least likely to be run; this one needs neither and answers the question that
 * actually recurs — somebody adds a controller method six months from now and does not think about
 * who may call it. A missing {@code @PreAuthorize} is invisible in review and silent at runtime,
 * because the filter chain still lets any valid token through and the endpoint simply works for
 * everyone. It is a class of bug that ships.
 *
 * <p>Reads are not covered. Most of them are public by design, and the ones that are not are on
 * {@code /user/**}, which the chain closes wholesale — a rule this test cannot see and should not
 * duplicate.
 *
 * <p>The test says nothing about whether a rule is the <em>right</em> rule. That is what review is
 * for. It only insists somebody decided.
 */
class EveryWriteIsAuthorisedTest {

  private static final String CONTROLLERS = "com.documan.controllers";

  /**
   * Methods that act on the token's owner and take no identifier to abuse.
   *
   * <p>{@code updateMyProfile} reads {@code CurrentUser.requireId()} for the actor, so there is
   * nothing to authorise beyond being signed in, which the chain already requires. Adding a rule
   * would be stating that a person may edit themselves.
   */
  private static final Set<String> ACTS_ON_SELF_ONLY =
      Set.of("UserController#updateMyProfile");

  @Test
  @DisplayName("every POST, PUT, PATCH and DELETE has a @PreAuthorize")
  void everyWriteIsAuthorised() throws Exception {
    List<String> ungated = new ArrayList<>();

    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
    Set<BeanDefinition> found = scanner.findCandidateComponents(CONTROLLERS);
    assertThat(found)
        .as("no controllers scanned — the package moved and this test stopped checking anything")
        .isNotEmpty();

    for (BeanDefinition definition : found) {
      Class<?> controller = Class.forName(definition.getBeanClassName());
      for (Method method : controller.getDeclaredMethods()) {
        if (!method.getDeclaringClass().equals(controller) || !writes(method)) {
          continue;
        }
        String name = controller.getSimpleName() + "#" + method.getName();
        if (ACTS_ON_SELF_ONLY.contains(name)) {
          continue;
        }
        if (!AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)) {
          ungated.add(name);
        }
      }
    }

    assertThat(ungated)
        .as(
            "These change something and say nothing about who may. Add a @PreAuthorize, or add the"
                + " method to ACTS_ON_SELF_ONLY with the reason it needs none.")
        .isEmpty();
  }

  private static boolean writes(Method method) {
    return AnnotatedElementUtils.hasAnnotation(method, PostMapping.class)
        || AnnotatedElementUtils.hasAnnotation(method, PutMapping.class)
        || AnnotatedElementUtils.hasAnnotation(method, PatchMapping.class)
        || AnnotatedElementUtils.hasAnnotation(method, DeleteMapping.class);
  }
}
