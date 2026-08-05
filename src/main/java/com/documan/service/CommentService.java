// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.config.CacheConfig;
import com.documan.dao.CommentDao;
import com.documan.dao.PostDao;
import com.documan.dao.UserDao;
import com.documan.dto.request.CreateCommentRequest;
import com.documan.dto.request.UpdateCommentRequest;
import com.documan.dto.response.CommentResponse;
import com.documan.dto.response.PageResponse;
import com.documan.entity.Comment;
import com.documan.entity.Post;
import com.documan.entity.User;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.CommentMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentService {

  private final CommentDao commentDao;
  private final UserDao userDao;
  private final PostDao postDao;
  private final CommentMapper commentMapper;

  public CommentService(
      CommentDao commentDao, UserDao userDao, PostDao postDao, CommentMapper commentMapper) {
    this.commentDao = commentDao;
    this.userDao = userDao;
    this.postDao = postDao;
    this.commentMapper = commentMapper;
  }

  @Cacheable(cacheNames = CacheConfig.COMMENTS, key = "#commentId")
  public CommentResponse findById(Integer commentId) {
    return commentMapper.toResponse(requireComment(commentId));
  }

  public PageResponse<CommentResponse> findAll(Pageable pageable) {
    return PageResponse.from(commentDao.findAll(pageable).map(commentMapper::toResponse));
  }

  /**
   * An author or post with no comments yields an empty page. The previous implementation translated
   * "no rows" into an empty {@code Optional}, which the controller then rendered as 404.
   */
  public PageResponse<CommentResponse> findByUser(Integer userId, Pageable pageable) {
    requireUserExists(userId);
    return PageResponse.from(
        commentDao.findByUserId(userId, pageable).map(commentMapper::toResponse));
  }

  public PageResponse<CommentResponse> findByPost(Integer postId, Pageable pageable) {
    requirePostExists(postId);
    return PageResponse.from(
        commentDao.findByPostId(postId, pageable).map(commentMapper::toResponse));
  }

  @Transactional
  @CachePut(cacheNames = CacheConfig.COMMENTS, key = "#result.id()")
  public CommentResponse create(CreateCommentRequest request, Integer userId, Integer postId) {
    User author =
        userDao.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
    Post post =
        postDao.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post", postId));

    Comment comment = new Comment();
    comment.setContent(request.content());
    comment.setUser(author);
    comment.setPost(post);
    return commentMapper.toResponse(commentDao.save(comment));
  }

  @Transactional
  @CachePut(cacheNames = CacheConfig.COMMENTS, key = "#commentId")
  public CommentResponse update(Integer commentId, UpdateCommentRequest request) {
    Comment comment = requireComment(commentId);
    comment.setContent(request.content());
    return commentMapper.toResponse(commentDao.save(comment));
  }

  @Transactional
  @CacheEvict(cacheNames = CacheConfig.COMMENTS, key = "#commentId")
  public void delete(Integer commentId) {
    commentDao.delete(requireComment(commentId));
  }

  private Comment requireComment(Integer commentId) {
    return commentDao
        .findWithUserById(commentId)
        .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
  }

  private void requireUserExists(Integer userId) {
    if (!userDao.existsById(userId)) {
      throw new ResourceNotFoundException("User", userId);
    }
  }

  private void requirePostExists(Integer postId) {
    if (!postDao.existsById(postId)) {
      throw new ResourceNotFoundException("Post", postId);
    }
  }
}
