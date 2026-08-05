// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.search.outbox;

import com.documan.entity.Comment;
import com.documan.entity.File;
import com.documan.entity.Post;
import com.documan.entity.Subject;
import com.documan.search.AggregateType;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.stereotype.Component;

/**
 * Captures entity lifecycle changes into {@link SearchDirtyBuffer}.
 *
 * <p>Using JPA lifecycle callbacks rather than a call in each of the twenty-odd mutating service
 * methods is deliberate. This codebase has already accumulated two bugs from the "remember to also
 * do X here" pattern, and one of them is directly relevant: {@code PostService.delete} removes
 * comments through {@code cascade = REMOVE}, so {@code CommentService.delete} is never invoked.
 * Hibernate issues an individual delete per comment, each firing {@link PostRemove} here, so the
 * cascade needs no special handling.
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
      case Post post -> buffer.markDirty(AggregateType.POST, post.getId());
      case Comment comment -> buffer.markDirty(AggregateType.COMMENT, comment.getId());
      case File file -> buffer.markDirty(AggregateType.FILE, file.getId());
      case Subject subject -> buffer.markDirty(AggregateType.SUBJECT, subject.getId());
      default -> {
        // Users and reference tables are not indexed; their effect on documents is handled by
        // explicit fan-out where a denormalised field actually changed.
      }
    }
  }
}
