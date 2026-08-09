// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.outbox;

import com.documan.entity.File;
import com.documan.search.AggregateType;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.stereotype.Component;

/**
 * Captures entity lifecycle changes into {@link SearchDirtyBuffer}.
 *
 * <p>Using JPA lifecycle callbacks rather than a call in each mutating service method is
 * deliberate: this codebase has already accumulated two bugs from the "remember to also do X here"
 * pattern. A file removed by a cascade fires {@link PostRemove} here like any other, so the cascade
 * needs no special handling.
 *
 * <p>{@link PostPersist} sees a populated identifier because every entity uses {@code IDENTITY}
 * generation, so the insert and the id assignment both happen during {@code persist()}.
 *
 * <p>These callbacks deliberately do <em>not</em> fire for the four {@code @Modifying} bulk counter
 * updates, which bypass Hibernate entirely. Those are enqueued explicitly, and with a debounce,
 * from the vote and favourite services.
 *
 * <p>Registered as a Spring bean; Boot configures Hibernate's {@code SpringBeanContainer}, so
 * constructor injection works.
 */
@Component
public class SearchEntityListener {

  private final SearchDirtyBuffer buffer;

  public SearchEntityListener(SearchDirtyBuffer buffer) {
    this.buffer = buffer;
  }

  @PostPersist
  @PostUpdate
  @PostRemove
  public void onChange(Object entity) {
    switch (entity) {
      case File file -> buffer.markDirty(AggregateType.FILE, file.getId());
      default -> {
        // Nothing else is indexed. A file document denormalises its subject's name and code, but
        // that is not repaired from here: SubjectService.update fans out to the subject's files
        // explicitly, and only when one of the copied fields actually changed.
      }
    }
  }
}
