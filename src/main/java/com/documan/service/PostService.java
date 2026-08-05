// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.config.CacheConfig;
import com.documan.dao.PostDao;
import com.documan.dao.UserDao;
import com.documan.dto.request.CreatePostRequest;
import com.documan.dto.request.UpdatePostRequest;
import com.documan.dto.response.PageResponse;
import com.documan.dto.response.PostResponse;
import com.documan.entity.Post;
import com.documan.entity.User;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.PostMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {

  private final PostDao postDao;
  private final UserDao userDao;
  private final PostMapper postMapper;

  public PostService(PostDao postDao, UserDao userDao, PostMapper postMapper) {
    this.postDao = postDao;
    this.userDao = userDao;
    this.postMapper = postMapper;
  }

  @Cacheable(cacheNames = CacheConfig.POSTS, key = "#postId")
  public PostResponse findById(Integer postId) {
    return postMapper.toResponse(requirePost(postId));
  }

  /** Previously returned every post in the table with no upper bound. */
  public PageResponse<PostResponse> findAll(Pageable pageable) {
    return PageResponse.from(postDao.findAll(pageable).map(postMapper::toResponse));
  }

  public PageResponse<PostResponse> findByUser(Integer userId, Pageable pageable) {
    requireUserExists(userId);
    return PageResponse.from(postDao.findByUserId(userId, pageable).map(postMapper::toResponse));
  }

  @Transactional
  @CachePut(cacheNames = CacheConfig.POSTS, key = "#result.id()")
  public PostResponse create(CreatePostRequest request, Integer userId) {
    User author = requireUser(userId);

    Post post = new Post();
    post.setTitle(request.title());
    post.setDescription(request.description());
    post.setContent(request.content());
    post.setUser(author);
    return postMapper.toResponse(postDao.save(post));
  }

  @Transactional
  @CachePut(cacheNames = CacheConfig.POSTS, key = "#postId")
  public PostResponse update(Integer postId, UpdatePostRequest request) {
    Post post = requirePost(postId);
    post.setTitle(request.title());
    post.setDescription(request.description());
    post.setContent(request.content());
    return postMapper.toResponse(postDao.save(post));
  }

  /**
   * Evicts the post entry. The previous implementation evicted {@code COMMENT{id}} here, which both
   * left a stale post cached and could drop an unrelated comment sharing the numeric id.
   */
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.POSTS, key = "#postId")
  public void delete(Integer postId) {
    Post post = requirePost(postId);
    postDao.delete(post);
  }

  private Post requirePost(Integer postId) {
    return postDao
        .findWithUserById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
  }

  private User requireUser(Integer userId) {
    return userDao
        .findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", userId));
  }

  private void requireUserExists(Integer userId) {
    if (!userDao.existsById(userId)) {
      throw new ResourceNotFoundException("User", userId);
    }
  }
}
