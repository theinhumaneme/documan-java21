// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.dto.request.MoveFilesRequest;
import com.documan.dto.request.RenameFileRequest;
import com.documan.dto.response.FileResponse;
import com.documan.dto.response.PageResponse;
import com.documan.service.FileService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/file")
public class FileController {

  private final FileService fileService;

  public FileController(FileService fileService) {
    this.fileService = fileService;
  }

  /** Everything in the subject, whichever folder it sits in. */
  @GetMapping("/subject")
  public PageResponse<FileResponse> getSubjectFiles(
      @RequestParam("subjectId") Integer subjectId,
      @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC)
          Pageable pageable) {
    return fileService.findBySubject(subjectId, pageable);
  }

  /** One folder's contents. */
  @GetMapping("/folder")
  public PageResponse<FileResponse> getFolderFiles(
      @RequestParam("folderId") Integer folderId,
      @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC)
          Pageable pageable) {
    return fileService.findByFolder(folderId, pageable);
  }

  /**
   * A folder, not a subject: the folder already says which subject the file belongs to.
   *
   * <p>And because it does, it also says which address the upload lands at, which is the whole
   * question {@link com.documan.security.Permissions#mayEditFolder} answers. {@code
   * FileService.upload} still takes no actor — it does not need one, since nothing about the stored
   * file records who put it there — so the check lives here rather than being threaded through.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@permissions.mayEditFolder(#folderId)")
  public FileResponse createFile(
      @RequestParam("file") MultipartFile file, @RequestParam("folderId") Integer folderId) {
    return fileService.upload(file, folderId);
  }

  /**
   * Moves one or more files to a folder, or to a subject's root.
   *
   * <p>A body rather than query parameters because the list of ids is the payload, and the batch is
   * one transaction — so it is one request, not one per file.
   */
  @PatchMapping("/move")
  @PreAuthorize("@permissions.mayMoveFiles(#request.fileIds(), #request.folderId())")
  public List<FileResponse> moveFiles(@Valid @RequestBody MoveFilesRequest request) {
    return fileService.move(request);
  }

  @PatchMapping("/rename")
  @PreAuthorize("@permissions.mayEditFile(#fileId)")
  public FileResponse renameFile(
      @RequestParam("fileId") Integer fileId, @Valid @RequestBody RenameFileRequest request) {
    return fileService.rename(fileId, request);
  }

  /**
   * Redirects to a signed URL that saves the file instead of displaying it.
   *
   * <p>A redirect rather than streaming the bytes through this service: the download still comes
   * from Cloudflare's edge, so a library of several gigabytes does not become this application's
   * egress bill. Viewing a file needs none of this — the public {@code objectURL} on the response
   * already renders inline, which is exactly what a viewer wants.
   */
  @GetMapping("/download")
  public ResponseEntity<Void> downloadFile(@RequestParam("fileId") Integer fileId) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(fileService.downloadUrl(fileId)))
        .build();
  }

  /** Names the stored object rather than the row, so the check resolves it the same way. */
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@permissions.mayEditObject(#objectUID)")
  public void deleteFile(@RequestParam("objectUID") String objectUID) {
    fileService.delete(objectUID);
  }
}
