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
import com.documan.dto.request.MoveFilesRequest;
import com.documan.dto.request.RenameFileRequest;
import com.documan.dto.response.FileResponse;
import com.documan.dto.response.PageResponse;
import com.documan.entity.File;
import com.documan.entity.Folder;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.FileMapper;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** Owns file metadata; delegates byte movement to {@link CloudflareR2Service}. */
@Service
@Transactional(readOnly = true)
public class FileService {

  private static final Pattern UNSAFE_FILENAME_CHARS = Pattern.compile("[^A-Za-z0-9._-]+");
  private static final int MAX_FILENAME_LENGTH = 180;

  private final FileDao fileDao;
  private final SubjectDao subjectDao;
  private final FolderDao folderDao;
  private final CloudflareR2Service storage;
  private final FileMapper fileMapper;
  private final FileRecorder recorder;

  @Value("${cloudflare.r2.files-bucket-public-access-url}")
  private String publicAccessUrl;

  public FileService(
      FileDao fileDao,
      SubjectDao subjectDao,
      FolderDao folderDao,
      CloudflareR2Service storage,
      FileMapper fileMapper,
      FileRecorder recorder) {
    this.fileDao = fileDao;
    this.subjectDao = subjectDao;
    this.folderDao = folderDao;
    this.storage = storage;
    this.fileMapper = fileMapper;
    this.recorder = recorder;
  }

  /** Everything filed against a subject, across all of its folders. */
  public PageResponse<FileResponse> findBySubject(Integer subjectId, Pageable pageable) {
    if (!subjectDao.existsById(subjectId)) {
      throw new ResourceNotFoundException("Subject", subjectId);
    }
    return PageResponse.from(
        fileDao.findBySubjectId(subjectId, pageable).map(fileMapper::toResponse));
  }

  /** One folder's contents. */
  public PageResponse<FileResponse> findByFolder(Integer folderId, Pageable pageable) {
    if (!folderDao.existsById(folderId)) {
      throw new ResourceNotFoundException("Folder", folderId);
    }
    return PageResponse.from(
        fileDao.findByFolderId(folderId, pageable).map(fileMapper::toResponse));
  }

  /**
   * Uploads then records. If the metadata write fails the uploaded object is removed, so a failure
   * part-way through no longer leaves an unreferenced object in the bucket.
   *
   * <p>Takes a folder, not a subject. Every file lives in a folder, and the folder already knows
   * which subject it belongs to — asking for both would introduce a pair that can disagree, and
   * then a check to catch it. A subject with no folders yet cannot receive an upload, which is the
   * intended answer: make somewhere to put it first.
   *
   * <p><b>Deliberately not {@code @Transactional}.</b> The transfer to Cloudflare is the long part
   * and needs no transaction; wrapping it in one held a pooled connection for its whole duration and
   * turned a bulk upload into a pool outage for every other request. The two database steps are
   * {@link FileRecorder}'s, each brief and each committing on its own, with the connection returned
   * to the pool while the bytes move.
   *
   * <p>The window this opens is real and is the reason for the {@code deleteQuietly}: between the
   * PUT and the insert there is an object in the bucket with no row naming it. A crash in that
   * window leaks it, where before it would have rolled back. That is the trade — an occasional
   * orphaned object against a connection pool that survives someone uploading a folder — and the
   * orphan is the cheaper failure. The README already notes there is no reconciliation job for
   * objects orphaned out of band; this widens the case for one slightly.
   */
  public FileResponse upload(MultipartFile file, Integer folderId) {
    Folder folder = recorder.destination(folderId);

    String originalName =
        StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "unnamed";
    String objectName = buildObjectName(originalName);

    storage.uploadFile(objectName, file);
    try {
      File entity = new File();
      entity.setName(originalName);
      entity.setObjectName(objectName);
      entity.setObjectURL(publicUrlFor(objectName));
      entity.setSize(file.getSize());
      entity.setFolder(folder);
      entity.setSubject(folder.getSubject());
      return fileMapper.toResponse(recorder.record(entity));
    } catch (RuntimeException e) {
      storage.deleteQuietly(objectName);
      throw e;
    }
  }

  /**
   * Moves files into a folder, which may belong to another subject.
   *
   * <p>Metadata only. The object key is a random prefix with no path in it, so a file moving
   * between subjects does not need its bytes copied — one column update per file, and any URL
   * already shared keeps working. This is the property that makes cross-subject moves cheap enough
   * to offer.
   *
   * <p>The whole batch is one transaction: a move of twenty files that fails on the nineteenth
   * leaves nothing half-done, which matters because there is no undo in the interface.
   *
   * <p>Saved with a flush so the search index actually follows. {@code SearchEntityListener} raises
   * the dirty key from {@code @PostUpdate}, and that callback only runs when the UPDATE is issued —
   * which, left to the commit, happens after {@code SearchDirtyBuffer} has already written the
   * outbox and is therefore too late to be picked up. Flushing here puts the UPDATE inside this
   * method, so the key is enqueued by the same transaction that moved the row. A move that changes
   * subject also changes the denormalised subject fields on the document, so missing this leaves
   * search reporting the old address indefinitely.
   */
  @Transactional
  public List<FileResponse> move(MoveFilesRequest request) {
    Folder destination =
        folderDao
            .findById(request.folderId())
            .orElseThrow(() -> new ResourceNotFoundException("Folder", request.folderId()));

    List<File> files = fileDao.findAllById(request.fileIds());
    if (files.size() != request.fileIds().size()) {
      throw new ResourceNotFoundException("One or more of the files to move does not exist");
    }

    for (File file : files) {
      // Both columns are set from the destination folder, so they cannot disagree.
      file.setFolder(destination);
      file.setSubject(destination.getSubject());
    }
    List<File> saved = fileDao.saveAll(files);
    fileDao.flush();
    return saved.stream().map(fileMapper::toResponse).toList();
  }

  /**
   * Renames a file.
   *
   * <p>Only the display name. The object key stays as it was uploaded, so this is one column and no
   * traffic to the bucket.
   */
  @Transactional
  public FileResponse rename(Integer fileId, RenameFileRequest request) {
    File file =
        fileDao.findById(fileId).orElseThrow(() -> new ResourceNotFoundException("File", fileId));
    file.setName(request.name().trim());
    // saveAndFlush, not save: the UPDATE has to be issued now so @PostUpdate raises the dirty key
    // while SearchDirtyBuffer is still collecting. See move() for why.
    return fileMapper.toResponse(fileDao.saveAndFlush(file));
  }

  /**
   * Removes both the stored object and its metadata row. The previous implementation deleted only
   * the R2 object and left the {@code file} row behind.
   *
   * <p>Flushed for the same reason {@link #rename} is: {@code @PostRemove} fires when the DELETE is
   * issued, and left to the commit that happens after the outbox has already been written — so
   * without this the row goes but the search document stays, and search keeps offering a file that
   * no longer exists.
   */
  @Transactional
  public void delete(String objectName) {
    File file =
        fileDao
            .findByObjectName(objectName)
            .orElseThrow(() -> new ResourceNotFoundException("File '%s'".formatted(objectName)));
    storage.deleteFile(objectName);
    fileDao.delete(file);
    fileDao.flush();
  }

  /**
   * A signed URL that saves the file under the name a reader knows it by.
   *
   * <p>Two things this fixes that a plain link to {@code objectURL} cannot. A browser shows a PDF
   * or an image inline rather than saving it, because R2 sends no {@code Content-Disposition} and
   * the {@code download} attribute does not apply across origins. And the object key is a random
   * prefix plus a sanitised, lower-cased form of the original name, so even a forced save would
   * produce {@code 4b775c93..._unit-3-notes.pdf} rather than {@code Unit 3 Notes.pdf}.
   *
   * <p>Ten minutes is long enough to start a download of the largest file here and short enough
   * that a shared link stops working before it is useful to anyone the sharer did not intend.
   */
  public String downloadUrl(Integer fileId) {
    File file =
        fileDao.findById(fileId).orElseThrow(() -> new ResourceNotFoundException("File", fileId));
    return storage.presignedDownloadUrl(
        file.getObjectName(), file.getName(), Duration.ofMinutes(10));
  }

  /**
   * The public download URL for an object.
   *
   * <p>Any trailing slash on the configured base is dropped first. Cloudflare shows a bucket's
   * public URL with one, so pasting it in verbatim is the expected thing to do, and joining it
   * naively produced a double slash in every stored {@code object_url}.
   */
  private String publicUrlFor(String objectName) {
    String base =
        publicAccessUrl.endsWith("/")
            ? publicAccessUrl.substring(0, publicAccessUrl.length() - 1)
            : publicAccessUrl;
    return "%s/%s".formatted(base, objectName);
  }

  /**
   * Client-supplied names only ever reach the bucket as a sanitised suffix behind a random prefix,
   * so they cannot collide or smuggle path separators into the key.
   */
  private static String buildObjectName(String originalFilename) {
    String cleaned = StringUtils.getFilename(originalFilename);
    if (!StringUtils.hasText(cleaned)) {
      cleaned = "unnamed";
    }
    cleaned = UNSAFE_FILENAME_CHARS.matcher(cleaned).replaceAll("-");
    if (cleaned.length() > MAX_FILENAME_LENGTH) {
      cleaned = cleaned.substring(0, MAX_FILENAME_LENGTH);
    }
    String prefix = UUID.randomUUID().toString().replace("-", "");
    return "%s_%s".formatted(prefix, cleaned.toLowerCase(Locale.ROOT));
  }
}
