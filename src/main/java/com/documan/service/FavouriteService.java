// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.config.CacheConfig;
import com.documan.dao.FileDao;
import com.documan.dao.FileFavouriteDao;
import com.documan.dao.PostDao;
import com.documan.dao.PostFavouriteDao;
import com.documan.dao.UserDao;
import com.documan.dto.response.FileResponse;
import com.documan.dto.response.PostResponse;
import com.documan.entity.File;
import com.documan.entity.FileFavourite;
import com.documan.entity.Post;
import com.documan.entity.PostFavourite;
import com.documan.entity.User;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.FileMapper;
import com.documan.mapper.PostMapper;
import com.documan.search.AggregateType;
import com.documan.search.outbox.SearchDirtyBuffer;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Favourites follow the same shape as votes: a join-table row plus an atomic counter update, rather
 * than loading and diffing a {@code @ManyToMany} collection of every user who favourited the item.
 *
 * <p>File favourites had a model and a join table but no reachable code path; they are wired up
 * here alongside post favourites.
 */
@Service
public class FavouriteService {

  private final PostDao postDao;
  private final FileDao fileDao;
  private final UserDao userDao;
  private final PostFavouriteDao postFavouriteDao;
  private final FileFavouriteDao fileFavouriteDao;
  private final PostMapper postMapper;
  private final FileMapper fileMapper;
  private final SearchDirtyBuffer dirtyBuffer;

  public FavouriteService(
      PostDao postDao,
      FileDao fileDao,
      UserDao userDao,
      PostFavouriteDao postFavouriteDao,
      FileFavouriteDao fileFavouriteDao,
      PostMapper postMapper,
      FileMapper fileMapper,
      SearchDirtyBuffer dirtyBuffer) {
    this.postDao = postDao;
    this.fileDao = fileDao;
    this.userDao = userDao;
    this.postFavouriteDao = postFavouriteDao;
    this.fileFavouriteDao = fileFavouriteDao;
    this.postMapper = postMapper;
    this.fileMapper = fileMapper;
    this.dirtyBuffer = dirtyBuffer;
  }

  /** Idempotent: favouriting an already-favourited post leaves the tally unchanged. */
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.POSTS, key = "#postId")
  public PostResponse favouritePost(Integer postId, Integer userId) {
    Post post = requirePost(postId);
    User user = requireUser(userId);

    if (postFavouriteDao.existsByPostIdAndUserId(postId, userId)) {
      return postMapper.toResponse(post);
    }
    postFavouriteDao.save(new PostFavourite(post, user));
    postDao.applyFavouriteDelta(postId, 1);
    dirtyBuffer.markCounterDirty(AggregateType.POST, postId);
    return postMapper.toResponse(requirePost(postId));
  }

  @Transactional
  @CacheEvict(cacheNames = CacheConfig.POSTS, key = "#postId")
  public PostResponse removeFavouritePost(Integer postId, Integer userId) {
    Optional<PostFavourite> existing = postFavouriteDao.findByPostIdAndUserId(postId, userId);
    if (existing.isEmpty()) {
      return postMapper.toResponse(requirePost(postId));
    }
    postFavouriteDao.delete(existing.get());
    postDao.applyFavouriteDelta(postId, -1);
    dirtyBuffer.markCounterDirty(AggregateType.POST, postId);
    return postMapper.toResponse(requirePost(postId));
  }

  @Transactional
  public FileResponse favouriteFile(Integer fileId, Integer userId) {
    File file = requireFile(fileId);
    User user = requireUser(userId);

    if (fileFavouriteDao.existsByFileIdAndUserId(fileId, userId)) {
      return fileMapper.toResponse(file);
    }
    fileFavouriteDao.save(new FileFavourite(file, user));
    fileDao.applyFavouriteDelta(fileId, 1);
    dirtyBuffer.markCounterDirty(AggregateType.FILE, fileId);
    return fileMapper.toResponse(requireFile(fileId));
  }

  @Transactional
  public FileResponse removeFavouriteFile(Integer fileId, Integer userId) {
    Optional<FileFavourite> existing = fileFavouriteDao.findByFileIdAndUserId(fileId, userId);
    if (existing.isEmpty()) {
      return fileMapper.toResponse(requireFile(fileId));
    }
    fileFavouriteDao.delete(existing.get());
    fileDao.applyFavouriteDelta(fileId, -1);
    dirtyBuffer.markCounterDirty(AggregateType.FILE, fileId);
    return fileMapper.toResponse(requireFile(fileId));
  }

  private Post requirePost(Integer postId) {
    return postDao
        .findWithUserById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
  }

  private File requireFile(Integer fileId) {
    return fileDao
        .findById(fileId)
        .orElseThrow(() -> new ResourceNotFoundException("File", fileId));
  }

  private User requireUser(Integer userId) {
    return userDao
        .findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", userId));
  }
}
