// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.controllers;

import com.documan.dto.response.FileResponse;
import com.documan.dto.response.PageResponse;
import com.documan.service.FileService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/file")
public class FileController {

  private final FileService fileService;

  public FileController(FileService fileService) {
    this.fileService = fileService;
  }

  @GetMapping("/subject")
  public PageResponse<FileResponse> getSubjectFiles(
      @RequestParam("subjectId") Integer subjectId,
      @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC)
          Pageable pageable) {
    return fileService.findBySubject(subjectId, pageable);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FileResponse createFile(
      @RequestParam("file") MultipartFile file, @RequestParam("subjectId") Integer subjectId) {
    return fileService.upload(file, subjectId);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteFile(@RequestParam("objectUID") String objectUID) {
    fileService.delete(objectUID);
  }
}
