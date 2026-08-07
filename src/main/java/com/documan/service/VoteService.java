// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.config.CacheConfig;
import com.documan.dao.CommentDao;
import com.documan.dao.CommentVoteDao;
import com.documan.dao.PostDao;
import com.documan.dao.PostVoteDao;
import com.documan.dao.UserDao;
import com.documan.dto.response.CommentResponse;
import com.documan.dto.response.PostResponse;
import com.documan.entity.Comment;
import com.documan.entity.CommentVote;
import com.documan.entity.Post;
import com.documan.entity.PostVote;
import com.documan.entity.User;
import com.documan.entity.Votable;
import com.documan.entity.VoteType;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.CommentMapper;
import com.documan.mapper.PostMapper;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Voting is the hottest write path in the service, and it was previously the most expensive: each
 * vote loaded {@code post.upvotedUsers} and {@code post.downvotedUsers} in full — every user who
 * had ever voted on that post — mutated the in-memory collections, and let Hibernate diff them.
 *
 * <p>Now a vote touches exactly two rows: an upsert into the join table and one atomic counter
 * update, neither of which scales with the number of existing voters.
 */
@Service
public class VoteService {

  private final PostDao postDao;
  private final CommentDao commentDao;
  private final UserDao userDao;
  private final PostVoteDao postVoteDao;
  private final CommentVoteDao commentVoteDao;
  private final PostMapper postMapper;
  private final CommentMapper commentMapper;

  public VoteService(
      PostDao postDao,
      CommentDao commentDao,
      UserDao userDao,
      PostVoteDao postVoteDao,
      CommentVoteDao commentVoteDao,
      PostMapper postMapper,
      CommentMapper commentMapper) {
    this.postDao = postDao;
    this.commentDao = commentDao;
    this.userDao = userDao;
    this.postVoteDao = postVoteDao;
    this.commentVoteDao = commentVoteDao;
    this.postMapper = postMapper;
    this.commentMapper = commentMapper;
  }

  /** Casting the same vote twice is a no-op rather than an error. */
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.POSTS, key = "#postId")
  public PostResponse votePost(Integer postId, Integer userId, VoteType voteType) {
    Post post = requirePostWithAuthor(postId);
    User user = requireUser(userId);

    Optional<PostVote> existing = postVoteDao.findByPostIdAndUserId(postId, userId);
    VoteDelta delta;
    if (existing.isPresent()) {
      PostVote vote = existing.get();
      if (vote.getVoteType() == voteType) {
        return postMapper.toResponse(post);
      }
      vote.setVoteType(voteType);
      postVoteDao.save(vote);
      delta = VoteDelta.switchedTo(voteType);
    } else {
      postVoteDao.save(new PostVote(post, user, voteType));
      delta = VoteDelta.added(voteType);
    }

    postDao.applyVoteDelta(postId, delta.up(), delta.down());
    return postMapper.toResponse(requirePostWithAuthor(postId));
  }

  /** Removing a vote that was never cast, or that pointed the other way, is a no-op. */
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.POSTS, key = "#postId")
  public PostResponse removeVotePost(Integer postId, Integer userId, VoteType voteType) {
    Optional<PostVote> existing = postVoteDao.findByPostIdAndUserId(postId, userId);
    if (existing.isEmpty() || existing.get().getVoteType() != voteType) {
      return postMapper.toResponse(requirePostWithAuthor(postId));
    }

    postVoteDao.delete(existing.get());
    VoteDelta delta = VoteDelta.removed(voteType);
    postDao.applyVoteDelta(postId, delta.up(), delta.down());
    return postMapper.toResponse(requirePostWithAuthor(postId));
  }

  @Transactional
  @CacheEvict(cacheNames = CacheConfig.COMMENTS, key = "#commentId")
  public CommentResponse voteComment(Integer commentId, Integer userId, VoteType voteType) {
    Comment comment = requireCommentWithAuthor(commentId);
    User user = requireUser(userId);

    Optional<CommentVote> existing = commentVoteDao.findByCommentIdAndUserId(commentId, userId);
    VoteDelta delta;
    if (existing.isPresent()) {
      CommentVote vote = existing.get();
      if (vote.getVoteType() == voteType) {
        return commentMapper.toResponse(comment);
      }
      vote.setVoteType(voteType);
      commentVoteDao.save(vote);
      delta = VoteDelta.switchedTo(voteType);
    } else {
      commentVoteDao.save(new CommentVote(comment, user, voteType));
      delta = VoteDelta.added(voteType);
    }

    commentDao.applyVoteDelta(commentId, delta.up(), delta.down());
    return commentMapper.toResponse(requireCommentWithAuthor(commentId));
  }

  @Transactional
  @CacheEvict(cacheNames = CacheConfig.COMMENTS, key = "#commentId")
  public CommentResponse removeVoteComment(Integer commentId, Integer userId, VoteType voteType) {
    Optional<CommentVote> existing = commentVoteDao.findByCommentIdAndUserId(commentId, userId);
    if (existing.isEmpty() || existing.get().getVoteType() != voteType) {
      return commentMapper.toResponse(requireCommentWithAuthor(commentId));
    }

    commentVoteDao.delete(existing.get());
    VoteDelta delta = VoteDelta.removed(voteType);
    commentDao.applyVoteDelta(commentId, delta.up(), delta.down());
    return commentMapper.toResponse(requireCommentWithAuthor(commentId));
  }

  /**
   * Reads the tallies off anything votable. The sealed hierarchy makes this switch exhaustive
   * without a default branch, replacing the unchecked {@code (T)} casts the previous generic
   * implementation needed.
   */
  public static long netScore(Votable votable) {
    return switch (votable) {
      case Post post -> post.getUpvoteCount() - post.getDownvoteCount();
      case Comment comment -> comment.getUpvoteCount() - comment.getDownvoteCount();
    };
  }

  private Post requirePostWithAuthor(Integer postId) {
    return postDao
        .findWithUserById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
  }

  private Comment requireCommentWithAuthor(Integer commentId) {
    return commentDao
        .findWithUserById(commentId)
        .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
  }

  private User requireUser(Integer userId) {
    return userDao
        .findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", userId));
  }
}
