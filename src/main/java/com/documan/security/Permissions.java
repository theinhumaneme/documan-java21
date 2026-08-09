// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.security;

import com.documan.dao.CommentDao;
import com.documan.dao.FileDao;
import com.documan.dao.FolderDao;
import com.documan.dao.PostDao;
import com.documan.entity.Comment;
import com.documan.entity.File;
import com.documan.entity.Folder;
import com.documan.entity.Post;
import com.documan.entity.RoleName;
import com.documan.entity.Subject;
import com.documan.entity.User;
import com.documan.exception.ResourceNotFoundException;
import com.documan.service.MaintainerScopeService;
import java.util.Collection;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every authorisation question the API asks, in one place.
 *
 * <p>Reached from {@code @PreAuthorize("@permissions.…")}, which is why the bean is named. The
 * alternative — mapping the role onto a {@code ROLE_*} authority and writing {@code hasRole} — was
 * not taken, for two reasons. The role is a column in {@code documan_user}, not a claim, so an
 * authority would mean a database read inside the filter chain on every request including the
 * anonymous GETs that are most of this service's traffic. And {@code hasRole} cannot express the
 * question that actually matters here: a maintainer's right is not to "edit the library" but to edit
 * one address in it, which needs the folder or file in hand before it can be answered.
 *
 * <p><b>Ranks, not sets.</b> {@link RoleName} orders the four roles, and everything here asks "at
 * least this rank". An administrator is therefore also a moderator and also a maintainer, which is
 * what the client already assumes — {@code accounts.ts} computes its gates the same way, and the two
 * have to agree or the interface offers what the service refuses.
 *
 * <p><b>Transactional, and it has to be.</b> {@code open-in-view} is off, so a lazy association read
 * outside a transaction throws. These methods walk file → subject → department, so the session must
 * still be open while they do; a check that fails with a {@code LazyInitializationException} would
 * surface as a 500 on a request that should have been a clean 403.
 *
 * <p><b>Absent means 404, not 403.</b> A missing folder throws {@link ResourceNotFoundException}
 * from here rather than returning false. Answering "forbidden" for something that does not exist
 * tells a caller their permissions are wrong when the id is, and it is the same answer they would
 * get for a resource they simply may not touch — two different problems wearing one status code.
 */
@Component("permissions")
@Transactional(readOnly = true)
public class Permissions {

  private final CurrentUser currentUser;
  private final MaintainerScopeService scopes;
  private final FolderDao folderDao;
  private final FileDao fileDao;
  private final PostDao postDao;
  private final CommentDao commentDao;

  public Permissions(
      CurrentUser currentUser,
      MaintainerScopeService scopes,
      FolderDao folderDao,
      FileDao fileDao,
      PostDao postDao,
      CommentDao commentDao) {
    this.currentUser = currentUser;
    this.scopes = scopes;
    this.folderDao = folderDao;
    this.fileDao = fileDao;
    this.postDao = postDao;
    this.commentDao = commentDao;
  }

  // ---------------------------------------------------------------- identity

  /** The caller's row, provisioning it on first sight. 401 when the request is anonymous. */
  private User me() {
    return currentUser.require();
  }

  private boolean atLeast(RoleName wanted) {
    return RoleName.of(me().getRole().getName()).rank() >= wanted.rank();
  }

  public boolean isAdmin() {
    return atLeast(RoleName.ADMIN);
  }

  public boolean isModerator() {
    return atLeast(RoleName.MODERATOR);
  }

  public boolean isMaintainer() {
    return atLeast(RoleName.MAINTAINER);
  }

  public boolean isSelf(Integer userId) {
    return userId != null && userId.equals(me().getId());
  }

  public boolean isSelfOrAdmin(Integer userId) {
    User user = me();
    return userId != null
        && (userId.equals(user.getId())
            || RoleName.of(user.getRole().getName()).rank() >= RoleName.ADMIN.rank());
  }

  public boolean isSelfOrModerator(Integer userId) {
    User user = me();
    return userId != null
        && (userId.equals(user.getId())
            || RoleName.of(user.getRole().getName()).rank() >= RoleName.MODERATOR.rank());
  }

  // ------------------------------------------------------------------- blog

  /**
   * Posting and commenting are granted per account, not by rank.
   *
   * <p>A moderator does not get them implicitly: the flags exist because being able to police the
   * blog and being allowed to write on it are different permissions, and a moderator whose posting
   * was withdrawn should stay withdrawn.
   */
  public boolean mayPost() {
    return me().isCanPost();
  }

  public boolean mayComment() {
    return me().isCanComment();
  }

  /** Flagging a new post as an announcement. The client offers this to administrators only. */
  public boolean mayAnnounce() {
    return isAdmin();
  }

  /**
   * Setting the announcement flag on an existing post to {@code wanted}.
   *
   * <p>Asks whether the flag is being <em>changed</em>, not what it is. An edit sends the whole post
   * back, so a moderator fixing a typo on an announcement resubmits {@code announcement: true}
   * without meaning anything by it — refusing that would make announcements uneditable by the very
   * people who moderate them, and the client is careful to preserve the flag precisely so an edit
   * does not silently clear it. Only a real change needs an administrator.
   */
  public boolean mayAnnounceOn(Integer postId, boolean wanted) {
    Post post =
        postDao.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post", postId));
    return post.isAnnouncement() == wanted || isAdmin();
  }

  /** The author, or anyone whose job is to police what authors write. */
  public boolean mayEditPost(Integer postId) {
    Post post =
        postDao
            .findWithUserById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
    return ownsOrModerates(post.getUser().getId());
  }

  public boolean mayEditComment(Integer commentId) {
    Comment comment =
        commentDao
            .findWithUserById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
    return ownsOrModerates(comment.getUser().getId());
  }

  private boolean ownsOrModerates(Integer authorId) {
    User user = me();
    return user.getId().equals(authorId)
        || RoleName.of(user.getRole().getName()).rank() >= RoleName.MODERATOR.rank();
  }

  // ---------------------------------------------------------------- library

  /**
   * Whether the caller may change what is filed against a subject.
   *
   * <p>A moderator and above is unrestricted — they look after the whole library, and narrowing them
   * to granted addresses would mean granting scopes to the people whose job is to have none. Below
   * that, only a maintainer, and only where a grant covers the subject's address. A regular reader
   * never qualifies, whatever grants they happen to hold: {@code maintainer_scope} rows outlive a
   * demotion, and reading them alone would let a demoted maintainer keep working.
   */
  public boolean mayEditSubject(Integer subjectId) {
    User user = me();
    int rank = RoleName.of(user.getRole().getName()).rank();
    if (rank >= RoleName.MODERATOR.rank()) {
      return true;
    }
    if (rank < RoleName.MAINTAINER.rank()) {
      return false;
    }
    return scopes.mayEditSubject(user.getId(), subjectId);
  }

  /** The same question about an address given directly, for creating a subject at one. */
  public boolean mayEditAddress(Integer departmentId, Integer yearId, Integer semesterId) {
    User user = me();
    int rank = RoleName.of(user.getRole().getName()).rank();
    if (rank >= RoleName.MODERATOR.rank()) {
      return true;
    }
    if (rank < RoleName.MAINTAINER.rank()) {
      return false;
    }
    return scopes.mayEdit(user.getId(), departmentId, yearId, semesterId);
  }

  public boolean mayEditFolder(Integer folderId) {
    return mayEditSubject(folder(folderId).getSubject().getId());
  }

  public boolean mayEditFile(Integer fileId) {
    File file =
        fileDao.findById(fileId).orElseThrow(() -> new ResourceNotFoundException("File", fileId));
    return mayEditSubject(subjectOf(file).getId());
  }

  /** Deletion names the stored object rather than the row; the rule is the same one. */
  public boolean mayEditObject(String objectName) {
    File file =
        fileDao
            .findByObjectName(objectName)
            .orElseThrow(
                () -> new ResourceNotFoundException("File '%s'".formatted(objectName)));
    return mayEditSubject(subjectOf(file).getId());
  }

  /**
   * A move needs the right to both ends.
   *
   * <p>Checking only the destination would let a maintainer pull material out of a department they
   * have no grant over; checking only the source would let them push it into one. Neither is a move
   * they are allowed to make, so both are required — and a batch is refused whole rather than
   * partly applied, which matches the single transaction the service performs.
   */
  public boolean mayMoveFiles(Collection<Integer> fileIds, Integer destinationFolderId) {
    if (!mayEditFolder(destinationFolderId)) {
      return false;
    }
    for (Integer fileId : fileIds) {
      if (!mayEditFile(fileId)) {
        return false;
      }
    }
    return true;
  }

  private Folder folder(Integer folderId) {
    return folderDao
        .findById(folderId)
        .orElseThrow(() -> new ResourceNotFoundException("Folder", folderId));
  }

  /**
   * A file's subject, preferring its folder's.
   *
   * <p>{@code file.subject_id} is denormalised from the folder and the two are kept in step by the
   * service, but the folder is the one a move updates first. Reading through it means a check can
   * never be answered from a stale copy of the address it is about.
   */
  private Subject subjectOf(File file) {
    Folder folder = file.getFolder();
    return folder != null ? folder.getSubject() : file.getSubject();
  }
}
