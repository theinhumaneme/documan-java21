// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.dao.FileDao;
import com.documan.dao.SubjectDao;
import com.documan.dto.response.FileResponse;
import com.documan.dto.response.PageResponse;
import com.documan.entity.File;
import com.documan.entity.Subject;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.FileMapper;
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
  private final CloudflareR2Service storage;
  private final FileMapper fileMapper;

  @Value("${cloudflare.r2.files-bucket-public-access-url}")
  private String publicAccessUrl;

  public FileService(
      FileDao fileDao, SubjectDao subjectDao, CloudflareR2Service storage, FileMapper fileMapper) {
    this.fileDao = fileDao;
    this.subjectDao = subjectDao;
    this.storage = storage;
    this.fileMapper = fileMapper;
  }

  public PageResponse<FileResponse> findBySubject(Integer subjectId, Pageable pageable) {
    if (!subjectDao.existsById(subjectId)) {
      throw new ResourceNotFoundException("Subject", subjectId);
    }
    return PageResponse.from(
        fileDao.findBySubjectId(subjectId, pageable).map(fileMapper::toResponse));
  }

  /**
   * Uploads then records. If the metadata write fails the uploaded object is removed, so a failure
   * part-way through no longer leaves an unreferenced object in the bucket.
   */
  @Transactional
  public FileResponse upload(MultipartFile file, Integer subjectId) {
    Subject subject =
        subjectDao
            .findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));

    String originalName =
        StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "unnamed";
    String objectName = buildObjectName(originalName);

    storage.uploadFile(objectName, file);
    try {
      File entity = new File();
      entity.setName(originalName);
      entity.setObjectName(objectName);
      entity.setObjectURL("%s/%s".formatted(publicAccessUrl, objectName));
      entity.setSize(file.getSize());
      entity.setSubject(subject);
      return fileMapper.toResponse(fileDao.save(entity));
    } catch (RuntimeException e) {
      storage.deleteQuietly(objectName);
      throw e;
    }
  }

  /**
   * Removes both the stored object and its metadata row. The previous implementation deleted only
   * the R2 object and left the {@code file} row behind.
   */
  @Transactional
  public void delete(String objectName) {
    File file =
        fileDao
            .findByObjectName(objectName)
            .orElseThrow(() -> new ResourceNotFoundException("File '%s'".formatted(objectName)));
    storage.deleteFile(objectName);
    fileDao.delete(file);
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
