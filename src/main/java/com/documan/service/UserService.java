// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.config.CacheConfig;
import com.documan.dao.*;
import com.documan.dto.request.CreateUserRequest;
import com.documan.dto.request.UpdateUserRequest;
import com.documan.dto.response.*;
import com.documan.entity.*;
import com.documan.exception.DuplicateResourceException;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.*;
import com.documan.search.AggregateType;
import com.documan.search.outbox.SearchDirtyBuffer;
import com.documan.search.outbox.SearchOutboxStore;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class UserService {

  /** New accounts always start on the lowest-privilege role. */
  private static final int DEFAULT_ROLE_ID = 1;

  private final UserDao userDao;
  private final SubjectDao subjectDao;
  private final DepartmentDao departmentDao;
  private final YearDao yearDao;
  private final SemesterDao semesterDao;
  private final RoleDao roleDao;
  private final PostDao postDao;
  private final CommentDao commentDao;
  private final PostVoteDao postVoteDao;
  private final CommentVoteDao commentVoteDao;
  private final PostFavouriteDao postFavouriteDao;
  private final FileFavouriteDao fileFavouriteDao;
  private final UserMapper userMapper;
  private final PostMapper postMapper;
  private final CommentMapper commentMapper;
  private final SubjectMapper subjectMapper;
  private final FileMapper fileMapper;
  private final SearchOutboxStore searchOutbox;
  private final CacheManager cacheManager;
  private final SearchDirtyBuffer dirtyBuffer;
  private final FileDao fileDao;

  public UserService(
      UserDao userDao,
      SubjectDao subjectDao,
      DepartmentDao departmentDao,
      YearDao yearDao,
      SemesterDao semesterDao,
      RoleDao roleDao,
      PostDao postDao,
      CommentDao commentDao,
      PostVoteDao postVoteDao,
      CommentVoteDao commentVoteDao,
      PostFavouriteDao postFavouriteDao,
      FileFavouriteDao fileFavouriteDao,
      UserMapper userMapper,
      PostMapper postMapper,
      CommentMapper commentMapper,
      SubjectMapper subjectMapper,
      FileMapper fileMapper,
      SearchOutboxStore searchOutbox,
      CacheManager cacheManager,
      SearchDirtyBuffer dirtyBuffer,
      FileDao fileDao) {
    this.userDao = userDao;
    this.subjectDao = subjectDao;
    this.departmentDao = departmentDao;
    this.yearDao = yearDao;
    this.semesterDao = semesterDao;
    this.roleDao = roleDao;
    this.postDao = postDao;
    this.commentDao = commentDao;
    this.postVoteDao = postVoteDao;
    this.commentVoteDao = commentVoteDao;
    this.postFavouriteDao = postFavouriteDao;
    this.fileFavouriteDao = fileFavouriteDao;
    this.userMapper = userMapper;
    this.postMapper = postMapper;
    this.commentMapper = commentMapper;
    this.subjectMapper = subjectMapper;
    this.fileMapper = fileMapper;
    this.searchOutbox = searchOutbox;
    this.cacheManager = cacheManager;
    this.dirtyBuffer = dirtyBuffer;
    this.fileDao = fileDao;
  }

  @Cacheable(cacheNames = CacheConfig.USERS, key = "#userId")
  public UserResponse findById(Integer userId) {
    return userMapper.toResponse(requireUser(userId));
  }

  public UserResponse findByUsername(String username) {
    return userDao
        .findByUsername(username)
        .map(userMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("User '%s'".formatted(username)));
  }

  public PageResponse<UserResponse> findAll(Pageable pageable) {
    return PageResponse.from(userDao.findAll(pageable).map(userMapper::toResponse));
  }

  @Transactional
  @CachePut(cacheNames = CacheConfig.USERS, key = "#result.id()")
  public UserResponse create(CreateUserRequest request) {
    if (userDao.existsByUsername(request.username())) {
      throw new DuplicateResourceException(
          "Username '%s' is already taken".formatted(request.username()));
    }
    if (userDao.existsByEmail(request.email())) {
      throw new DuplicateResourceException(
          "Email '%s' is already registered".formatted(request.email()));
    }

    User user = new User();
    user.setUsername(request.username());
    user.setPassword(request.password());
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setEmail(request.email());
    user.setAcceptedTermsOfService(request.acceptedTermsOfService());
    user.setDepartment(requireDepartment(request.departmentId()));
    user.setYear(requireYear(request.yearId()));
    user.setSemester(requireSemester(request.semesterId()));
    user.setRole(
        roleDao
            .findById(DEFAULT_ROLE_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Role", DEFAULT_ROLE_ID)));
    return userMapper.toResponse(userDao.save(user));
  }

  /**
   * Only fields present in the request are applied. In particular a null password leaves the stored
   * credential alone; the previous implementation overwrote it on every profile update, and never
   * copied the terms/posting/commenting flags at all.
   */
  @Transactional
  @CachePut(cacheNames = CacheConfig.USERS, key = "#userId")
  public UserResponse update(Integer userId, UpdateUserRequest request) {
    User user = requireUser(userId);
    String previousUsername = user.getUsername();

    if (userDao.existsByUsernameAndIdNot(request.username(), userId)) {
      throw new DuplicateResourceException(
          "Username '%s' is already taken".formatted(request.username()));
    }
    if (userDao.existsByEmailAndIdNot(request.email(), userId)) {
      throw new DuplicateResourceException(
          "Email '%s' is already registered".formatted(request.email()));
    }

    user.setUsername(request.username());
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setEmail(request.email());
    user.setDepartment(requireDepartment(request.departmentId()));
    user.setYear(requireYear(request.yearId()));
    user.setSemester(requireSemester(request.semesterId()));

    if (StringUtils.hasText(request.password())) {
      user.setPassword(request.password());
    }
    if (request.acceptedTermsOfService() != null) {
      user.setAcceptedTermsOfService(request.acceptedTermsOfService());
    }
    if (request.canPost() != null) {
      user.setCanPost(request.canPost());
    }
    if (request.canComment() != null) {
      user.setCanComment(request.canComment());
    }
    User saved = userDao.save(user);

    if (!previousUsername.equals(saved.getUsername())) {
      onUsernameChanged(userId);
    }
    return userMapper.toResponse(saved);
  }

  /**
   * The author's username is denormalised into every post and comment read model — both the search
   * documents and the cached {@code PostResponse}/{@code CommentResponse} entries.
   *
   * <p>The cache half was a live bug: only the {@code users} entry was evicted, so posts and
   * comments served the old username for the full ten-minute TTL. Search and cache are invalidated
   * from the same place so they cannot disagree with each other.
   */
  private void onUsernameChanged(Integer userId) {
    searchOutbox.markPostsOfUserDirty(userId);
    searchOutbox.markCommentsOfUserDirty(userId);

    Cache posts = cacheManager.getCache(CacheConfig.POSTS);
    Cache comments = cacheManager.getCache(CacheConfig.COMMENTS);
    if (posts != null) {
      postDao.findIdsByUserId(userId).forEach(posts::evict);
    }
    if (comments != null) {
      commentDao.findIdsByUserId(userId).forEach(comments::evict);
    }
  }

  /**
   * Deleting a user lets the database cascade away their vote and favourite rows, and nothing
   * decremented the tallies those rows contributed to, so post, comment and file counters
   * permanently over-counted. The contributions are withdrawn here, before the cascade removes the
   * evidence.
   *
   * <p>Withdrawing them through the same atomic delta statements the vote paths use also marks the
   * affected entities dirty, so the search index sees the corrected numbers rather than inheriting
   * the drift.
   *
   * <p>The set is small in practice: {@code post.user_id} and {@code comment.user_id} have no
   * cascade, so a user who has authored anything cannot be deleted at all.
   */
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.USERS, key = "#userId")
  public void delete(Integer userId) {
    User user = requireUser(userId);
    withdrawVotesAndFavourites(userId);
    userDao.delete(user);
  }

  private void withdrawVotesAndFavourites(Integer userId) {
    postVoteDao
        .findByUserId(userId)
        .forEach(
            vote -> {
              VoteDelta delta = VoteDelta.removed(vote.getVoteType());
              Integer postId = vote.getPost().getId();
              postDao.applyVoteDelta(postId, delta.up(), delta.down());
              evictAndMarkDirty(CacheConfig.POSTS, AggregateType.POST, postId);
            });

    commentVoteDao
        .findByUserId(userId)
        .forEach(
            vote -> {
              VoteDelta delta = VoteDelta.removed(vote.getVoteType());
              Integer commentId = vote.getComment().getId();
              commentDao.applyVoteDelta(commentId, delta.up(), delta.down());
              evictAndMarkDirty(CacheConfig.COMMENTS, AggregateType.COMMENT, commentId);
            });

    postFavouriteDao
        .findByUserId(userId)
        .forEach(
            favourite -> {
              Integer postId = favourite.getPost().getId();
              postDao.applyFavouriteDelta(postId, -1);
              evictAndMarkDirty(CacheConfig.POSTS, AggregateType.POST, postId);
            });

    fileFavouriteDao
        .findByUserId(userId)
        .forEach(
            favourite -> {
              Integer fileId = favourite.getFile().getId();
              fileDao.applyFavouriteDelta(fileId, -1);
              dirtyBuffer.markDirty(AggregateType.FILE, fileId);
            });
  }

  private void evictAndMarkDirty(String cacheName, AggregateType type, Integer id) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.evict(id);
    }
    dirtyBuffer.markDirty(type, id);
  }

  // ---------------------------------------------------------------------
  // Relationship views. These existed on the service before but were never
  // reachable through a controller, and each returned an unbounded list.
  // ---------------------------------------------------------------------

  public PageResponse<PostResponse> findPosts(Integer userId, Pageable pageable) {
    requireUserExists(userId);
    return PageResponse.from(postDao.findByUserId(userId, pageable).map(postMapper::toResponse));
  }

  public PageResponse<CommentResponse> findComments(Integer userId, Pageable pageable) {
    requireUserExists(userId);
    return PageResponse.from(
        commentDao.findByUserId(userId, pageable).map(commentMapper::toResponse));
  }

  public PageResponse<PostResponse> findFavouritePosts(Integer userId, Pageable pageable) {
    requireUserExists(userId);
    return PageResponse.from(
        postFavouriteDao.findFavouritePostsByUser(userId, pageable).map(postMapper::toResponse));
  }

  public PageResponse<FileResponse> findFavouriteFiles(Integer userId, Pageable pageable) {
    requireUserExists(userId);
    return PageResponse.from(
        fileFavouriteDao.findFavouriteFilesByUser(userId, pageable).map(fileMapper::toResponse));
  }

  public PageResponse<PostResponse> findVotedPosts(
      Integer userId, VoteType voteType, Pageable pageable) {
    requireUserExists(userId);
    return PageResponse.from(
        postVoteDao.findVotedPostsByUser(userId, voteType, pageable).map(postMapper::toResponse));
  }

  public PageResponse<CommentResponse> findVotedComments(
      Integer userId, VoteType voteType, Pageable pageable) {
    requireUserExists(userId);
    return PageResponse.from(
        commentVoteDao
            .findVotedCommentsByUser(userId, voteType, pageable)
            .map(commentMapper::toResponse));
  }

  /** The subjects taught for this user's department, year and semester. */
  public PageResponse<SubjectResponse> findSubjects(Integer userId, Pageable pageable) {
    User user = requireUser(userId);
    return PageResponse.from(
        subjectDao
            .findByDepartmentIdAndYearIdAndSemesterId(
                user.getDepartment().getId(),
                user.getYear().getId(),
                user.getSemester().getId(),
                pageable)
            .map(subjectMapper::toResponse));
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

  private Department requireDepartment(Integer id) {
    return departmentDao
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Department", id));
  }

  private Year requireYear(Integer id) {
    return yearDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("Year", id));
  }

  private Semester requireSemester(Integer id) {
    return semesterDao
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Semester", id));
  }
}
