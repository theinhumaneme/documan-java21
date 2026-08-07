// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.dto.request.CreateFolderRequest;
import com.documan.dto.request.UpdateFolderRequest;
import com.documan.dto.response.FolderResponse;
import com.documan.service.FolderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/folder")
public class FolderController {

  private final FolderService folderService;

  public FolderController(FolderService folderService) {
    this.folderService = folderService;
  }

  /**
   * A plain array, not a {@code PageResponse}. A subject holds a handful of folders, so paging
   * would make every client implement something it never needs — the same reasoning as the
   * reference lookups on department, year and semester.
   */
  @GetMapping
  public List<FolderResponse> getFolders(@RequestParam("subjectId") Integer subjectId) {
    return folderService.findBySubject(subjectId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FolderResponse createFolder(
      @RequestParam("subjectId") Integer subjectId,
      @Valid @RequestBody CreateFolderRequest request) {
    return folderService.create(subjectId, request);
  }

  @PutMapping
  public FolderResponse updateFolder(
      @RequestParam("folderId") Integer folderId, @Valid @RequestBody UpdateFolderRequest request) {
    return folderService.rename(folderId, request);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteFolder(@RequestParam("folderId") Integer folderId) {
    folderService.delete(folderId);
  }
}
