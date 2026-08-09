// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.dao.FileDao;
import com.documan.dao.FolderDao;
import com.documan.dao.SubjectDao;
import com.documan.dto.request.CreateFolderRequest;
import com.documan.dto.request.UpdateFolderRequest;
import com.documan.dto.response.FolderResponse;
import com.documan.entity.DefaultFolder;
import com.documan.entity.Folder;
import com.documan.entity.Subject;
import com.documan.exception.DuplicateResourceException;
import com.documan.exception.InvalidRequestException;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.FolderMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Owns the folders inside a subject.
 *
 * <p>Folders never move and never nest, so there is no tree to maintain here — only the rules about
 * which of them may be changed. Default folders are structural: they are what the subject was
 * created with, so they cannot be renamed or deleted. Custom folders belong to whoever added them.
 */
@Service
@Transactional(readOnly = true)
public class FolderService {

  private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");
  private static final int MAX_SLUG_LENGTH = 100;

  private final FolderDao folderDao;
  private final FileDao fileDao;
  private final SubjectDao subjectDao;
  private final FolderMapper folderMapper;

  public FolderService(
      FolderDao folderDao, FileDao fileDao, SubjectDao subjectDao, FolderMapper folderMapper) {
    this.folderDao = folderDao;
    this.fileDao = fileDao;
    this.subjectDao = subjectDao;
    this.folderMapper = folderMapper;
  }

  /**
   * Every folder of a subject, each with its file count.
   *
   * <p>A plain list rather than a {@code PageResponse}: a subject holds a handful of folders, and
   * paging a set that small would make every client implement paging it will never use. This
   * matches how the three reference lookups are served.
   *
   * <p>The lab folder is returned even when the subject no longer has a lab. Hiding it is the
   * client's job — the folder and its files still exist, and a server that omitted it would make
   * material unreachable rather than merely unlisted.
   */
  public List<FolderResponse> findBySubject(Integer subjectId) {
    requireSubject(subjectId);
    Map<Integer, Long> counts = fileCountsBySubject(subjectId);
    return folderDao.findBySubjectIdOrderByIdAsc(subjectId).stream()
        .map(folder -> folderMapper.toResponse(folder, counts.getOrDefault(folder.getId(), 0L)))
        .toList();
  }

  @Transactional
  public FolderResponse create(Integer subjectId, CreateFolderRequest request) {
    Subject subject = requireSubject(subjectId);
    String slug = slugify(request.name());
    if (folderDao.existsBySubjectIdAndSlug(subjectId, slug)) {
      throw new DuplicateResourceException(
          "Subject %d already has a folder '%s'".formatted(subjectId, slug));
    }
    Folder folder = new Folder();
    folder.setName(request.name().trim());
    folder.setSlug(slug);
    folder.setDefaultFolder(false);
    folder.setSubject(subject);
    return folderMapper.toResponse(folderDao.save(folder), 0L);
  }

  /** Renames a custom folder. The slug is left alone so existing links keep resolving. */
  @Transactional
  public FolderResponse rename(Integer folderId, UpdateFolderRequest request) {
    Folder folder = requireFolder(folderId);
    if (folder.isDefaultFolder()) {
      throw new DuplicateResourceException(
          "Folder '%s' is part of the subject's structure and cannot be renamed"
              .formatted(folder.getSlug()));
    }
    folder.setName(request.name().trim());
    Folder saved = folderDao.save(folder);
    return folderMapper.toResponse(saved, fileCount(saved.getId()));
  }

  /**
   * Deletes a custom, empty folder.
   *
   * <p>Non-empty is refused rather than cascaded. Deleting a folder is a tidying-up action, and
   * taking a dozen uploaded files with it is not what the person clicking it meant. Move the files
   * out first — which the interface can offer, because it knows the folder is not empty.
   */
  @Transactional
  public void delete(Integer folderId) {
    Folder folder = requireFolder(folderId);
    if (folder.isDefaultFolder()) {
      throw new DuplicateResourceException(
          "Folder '%s' is part of the subject's structure and cannot be deleted"
              .formatted(folder.getSlug()));
    }
    if (fileDao.existsByFolderId(folderId)) {
      throw new DuplicateResourceException(
          "Folder '%s' still holds files; move them out before deleting it"
              .formatted(folder.getSlug()));
    }
    folderDao.delete(folder);
  }

  /**
   * Creates the requested default folders for a newly created subject.
   *
   * <p>Called from {@code SubjectService.create} inside the same transaction, so a subject is never
   * visible without the folders it was asked for.
   *
   * <p>An unrecognised slug is rejected rather than ignored: a caller that misspells {@code unit-3}
   * should be told, not silently given a subject missing a folder they believe they created. {@link
   * DefaultFolder#LAB} is skipped for a subject with no lab, because the alternative is a folder
   * the client is then obliged to hide forever.
   */
  @Transactional
  public void provisionDefaults(Subject subject, Collection<String> slugs) {
    if (slugs == null || slugs.isEmpty()) {
      return;
    }
    Set<String> requested = Set.copyOf(slugs);
    for (String slug : requested) {
      if (DefaultFolder.bySlug(slug).isEmpty()) {
        throw new InvalidRequestException("'%s' is not one of the default folders".formatted(slug));
      }
    }

    // Iterated in enum declaration order rather than over the requested set, so identities are
    // assigned coursefiles → unit-1 … unit-5 → lab. A HashSet has no order, which made ids
    // arbitrary
    // and the DAO's ORDER BY id meaningless; this way the natural key order is already the order a
    // reader expects, and the client's sort becomes a confirmation rather than a repair.
    for (DefaultFolder template : DefaultFolder.values()) {
      if (!requested.contains(template.slug())) {
        continue;
      }
      if (template.requiresLab() && !subject.isLab()) {
        continue;
      }
      Folder folder = new Folder();
      folder.setName(template.displayName());
      folder.setSlug(template.slug());
      folder.setDefaultFolder(true);
      folder.setSubject(subject);
      folderDao.save(folder);
    }
  }

  /**
   * Clears a subject's folders so the subject itself can be removed.
   *
   * <p>Subject deletion has no JPA cascade, so without this the folder foreign key blocks it. Files
   * are untouched: a subject that still holds any cannot be deleted either way, and that check
   * belongs to the file's own foreign key rather than here.
   */
  @Transactional
  public void deleteAllForSubject(Integer subjectId) {
    folderDao.deleteBySubjectId(subjectId);
  }

  private long fileCount(Integer folderId) {
    return fileDao.existsByFolderId(folderId) ? countFilesIn(folderId) : 0L;
  }

  private long countFilesIn(Integer folderId) {
    Folder folder = requireFolder(folderId);
    return fileCountsBySubject(folder.getSubject().getId()).getOrDefault(folderId, 0L);
  }

  private Map<Integer, Long> fileCountsBySubject(Integer subjectId) {
    Map<Integer, Long> counts = new HashMap<>();
    for (FileDao.FolderFileCount row : fileDao.countByFolderForSubject(subjectId)) {
      counts.put(row.getFolderId(), row.getTotal());
    }
    return counts;
  }

  Folder requireFolder(Integer folderId) {
    return folderDao
        .findById(folderId)
        .orElseThrow(() -> new ResourceNotFoundException("Folder", folderId));
  }

  private Subject requireSubject(Integer subjectId) {
    return subjectDao
        .findById(subjectId)
        .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
  }

  /**
   * "Previous Papers" → {@code previous-papers}.
   *
   * <p>Derived rather than accepted from the caller so a custom folder cannot be given the slug of
   * a default one and shadow it. A collision surfaces as a 409 through the uniqueness check, which
   * is the right answer: a subject should not contain two folders a reader would call the same
   * thing.
   */
  private static String slugify(String name) {
    String slug = NON_SLUG_CHARS.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("-");
    slug = StringUtils.trimLeadingCharacter(slug, '-');
    slug = StringUtils.trimTrailingCharacter(slug, '-');
    if (slug.length() > MAX_SLUG_LENGTH) {
      slug = slug.substring(0, MAX_SLUG_LENGTH);
    }
    if (!StringUtils.hasText(slug)) {
      throw new InvalidRequestException(
          "'%s' does not contain any characters usable in a folder name".formatted(name));
    }
    return slug;
  }
}
