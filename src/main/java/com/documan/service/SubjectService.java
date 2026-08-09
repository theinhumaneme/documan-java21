// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.config.CacheConfig;
import com.documan.dao.DepartmentDao;
import com.documan.dao.SemesterDao;
import com.documan.dao.SubjectDao;
import com.documan.dao.YearDao;
import com.documan.dto.request.CreateSubjectRequest;
import com.documan.dto.request.UpdateSubjectRequest;
import com.documan.dto.response.PageResponse;
import com.documan.dto.response.SubjectResponse;
import com.documan.entity.Department;
import com.documan.entity.Semester;
import com.documan.entity.Subject;
import com.documan.entity.Year;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.SubjectMapper;
import com.documan.search.outbox.SearchOutboxStore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubjectService {

  private final SubjectDao subjectDao;
  private final DepartmentDao departmentDao;
  private final SemesterDao semesterDao;
  private final YearDao yearDao;
  private final SubjectMapper subjectMapper;
  private final SearchOutboxStore searchOutbox;
  private final FolderService folderService;

  public SubjectService(
      SubjectDao subjectDao,
      DepartmentDao departmentDao,
      SemesterDao semesterDao,
      YearDao yearDao,
      SubjectMapper subjectMapper,
      SearchOutboxStore searchOutbox,
      FolderService folderService) {
    this.subjectDao = subjectDao;
    this.departmentDao = departmentDao;
    this.semesterDao = semesterDao;
    this.yearDao = yearDao;
    this.subjectMapper = subjectMapper;
    this.searchOutbox = searchOutbox;
    this.folderService = folderService;
  }

  @Cacheable(cacheNames = CacheConfig.SUBJECTS, key = "#subjectId")
  public SubjectResponse findById(Integer subjectId) {
    return subjectMapper.toResponse(requireSubject(subjectId));
  }

  public PageResponse<SubjectResponse> findAll(Pageable pageable) {
    return PageResponse.from(subjectDao.findAll(pageable).map(subjectMapper::toResponse));
  }

  /**
   * The subjects filed at one address — the library's busiest read.
   *
   * <p>The address used to be validated first, three {@code findById} calls before the query that
   * answers the question, on every single browse. They exist to tell "no such department" apart from
   * "nothing filed there", which is worth saying — but a page that came back with rows has proved
   * the address exists, so asking beforehand is three round-trips to learn what the answer already
   * shows. They are now asked only when nothing came back, which is the sole case where the
   * distinction is visible. The common path loses three queries and the 404 is unchanged.
   *
   * <p>An empty page past the end of a real result set pays for the checks too. That is a request
   * for page nine of a two-page listing, and it is not worth a cheaper shape than this.
   */
  public PageResponse<SubjectResponse> findBy(
      Integer departmentId, Integer yearId, Integer semesterId, Pageable pageable) {
    Page<Subject> page =
        subjectDao.findByDepartmentIdAndYearIdAndSemesterId(
            departmentId, yearId, semesterId, pageable);
    if (page.isEmpty()) {
      requireDepartment(departmentId);
      requireYear(yearId);
      requireSemester(semesterId);
    }
    return PageResponse.from(page.map(subjectMapper::toResponse));
  }

  @Transactional
  @CachePut(cacheNames = CacheConfig.SUBJECTS, key = "#result.id()")
  public SubjectResponse create(CreateSubjectRequest request) {
    Subject subject = new Subject();
    apply(subject, request.name(), request.code(), request.lab(), request.theory());
    subject.setDepartment(requireDepartment(request.departmentId()));
    subject.setYear(requireYear(request.yearId()));
    subject.setSemester(requireSemester(request.semesterId()));
    Subject saved = subjectDao.save(subject);
    // Same transaction, so the subject is never visible without the folders it was created with.
    folderService.provisionDefaults(saved, request.defaultFolders());
    return subjectMapper.toResponse(saved);
  }

  @Transactional
  @CachePut(cacheNames = CacheConfig.SUBJECTS, key = "#subjectId")
  public SubjectResponse update(Integer subjectId, UpdateSubjectRequest request) {
    Subject subject = requireSubject(subjectId);
    // Snapshot the fields that file documents denormalise, so the fan-out below only fires when
    // one of them genuinely changed rather than on every save.
    String previousDenormalised = denormalisedIntoFiles(subject);

    apply(subject, request.name(), request.code(), request.lab(), request.theory());
    subject.setDepartment(requireDepartment(request.departmentId()));
    subject.setYear(requireYear(request.yearId()));
    subject.setSemester(requireSemester(request.semesterId()));
    Subject saved = subjectDao.save(subject);

    if (!previousDenormalised.equals(denormalisedIntoFiles(saved))) {
      // Every file of this subject carries a copy of these fields in its search document.
      searchOutbox.markFilesOfSubjectDirty(subjectId);
    }
    return subjectMapper.toResponse(saved);
  }

  /** The subject fields copied into each file document, as a comparable snapshot. */
  private static String denormalisedIntoFiles(Subject subject) {
    return String.join(
        "\u0000",
        subject.getName(),
        subject.getCode(),
        Boolean.toString(subject.isLab()),
        Boolean.toString(subject.isTheory()),
        String.valueOf(subject.getDepartment().getId()),
        subject.getDepartment().getName(),
        String.valueOf(subject.getYear().getId()),
        subject.getYear().getValue(),
        String.valueOf(subject.getSemester().getId()),
        subject.getSemester().getName());
  }

  /**
   * Folders go first. Subject deletion has no JPA cascade — {@code Post} is the only entity here
   * that declares one — so the folder foreign key would otherwise block every subject that was
   * created with default folders, which is most of them.
   *
   * <p>Files are not touched. A subject that still holds files cannot be deleted either way, and
   * that refusal belongs to the file's own foreign key rather than to a cascade written here:
   * silently deleting uploaded material as a side effect of removing a subject is not a trade worth
   * making. Both statements share this transaction, so a subject that turns out to hold files
   * leaves its folders intact.
   */
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.SUBJECTS, key = "#subjectId")
  public void delete(Integer subjectId) {
    Subject subject = requireSubject(subjectId);
    folderService.deleteAllForSubject(subjectId);
    subjectDao.delete(subject);
  }

  private static void apply(
      Subject subject, String name, String code, boolean lab, boolean theory) {
    subject.setName(name);
    subject.setCode(code);
    subject.setLab(lab);
    subject.setTheory(theory);
  }

  private Subject requireSubject(Integer subjectId) {
    return subjectDao
        .findById(subjectId)
        .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
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
