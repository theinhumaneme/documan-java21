// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.dao.FileDao;
import com.documan.dao.FolderDao;
import com.documan.entity.File;
import com.documan.entity.Folder;
import com.documan.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The two database halves of an upload, each in a transaction of its own.
 *
 * <p>A separate bean because {@code @Transactional} is applied by the proxy, and a call from one
 * method of {@link FileService} to another would not pass through it — the annotation would be
 * silently ignored, which is worse than not having it. The same reason {@code UserProvisioner}
 * exists.
 *
 * <p><b>Why the upload is split at all.</b> It used to be one transaction wrapping the whole
 * operation, R2 round-trip included, so an in-flight upload held a pooled connection for the
 * duration of the network transfer rather than for the row insert. Hikari allows twenty; a
 * maintainer uploading a folder of PDFs could hold all of them for seconds at a time, and every
 * other request — reading the blog, listing a subject — would queue behind bytes moving to
 * Cloudflare and start failing at the thirty-second connection timeout. The bulk import made this
 * concrete: sixteen workers at roughly 3 MB/s meant sixteen connections held for seconds each.
 *
 * <p>Nothing about the transfer needed a transaction. What needs one is reading the folder, and
 * writing the row; both are brief, and between them the connection goes back to the pool.
 */
@Component
public class FileRecorder {

  private final FileDao fileDao;
  private final FolderDao folderDao;

  public FileRecorder(FileDao fileDao, FolderDao folderDao) {
    this.fileDao = fileDao;
    this.folderDao = folderDao;
  }

  /**
   * The destination, with its subject already loaded.
   *
   * <p>The subject is fetched here rather than left lazy because the caller reads it after this
   * transaction has closed, and {@code open-in-view} is off — a lazy association touched then would
   * throw. Resolving it now is also what makes the folder's existence a 404 before a single byte is
   * sent to Cloudflare, instead of after.
   */
  @Transactional(readOnly = true)
  public Folder destination(Integer folderId) {
    Folder folder =
        folderDao
            .findById(folderId)
            .orElseThrow(() -> new ResourceNotFoundException("Folder", folderId));
    // Touches the proxy inside the transaction so the caller can read it outside one.
    folder.getSubject().getId();
    return folder;
  }

  /** The metadata row for an object already in the bucket. */
  @Transactional
  public File record(File entity) {
    return fileDao.save(entity);
  }
}
