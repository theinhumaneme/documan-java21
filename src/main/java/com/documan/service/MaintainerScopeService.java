// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.dao.DepartmentDao;
import com.documan.dao.MaintainerScopeDao;
import com.documan.dao.SemesterDao;
import com.documan.dao.SubjectDao;
import com.documan.dao.UserDao;
import com.documan.dao.YearDao;
import com.documan.dto.request.GrantScopeRequest;
import com.documan.dto.response.MaintainerScopeResponse;
import com.documan.entity.MaintainerScope;
import com.documan.entity.Subject;
import com.documan.exception.DuplicateResourceException;
import com.documan.exception.InvalidRequestException;
import com.documan.exception.ResourceNotFoundException;
import com.documan.mapper.MaintainerScopeMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who may change which part of the library.
 *
 * <p>A grant names a department, and optionally a year within it and a semester within that. The
 * three levels the college actually delegates at — a department, a year, a semester — are the three
 * shapes a row can take, and a null column means "all of them" rather than an enumerated set that
 * would quietly fail to cover a year added later.
 *
 * <p><strong>Not a security boundary yet.</strong> The application has no authentication: every
 * endpoint is permitted, and there is no principal to test a grant against. {@link #mayEdit} is the
 * rule these grants imply, written and ready, and the client uses it to decide what to offer. Until
 * an authenticated caller exists, someone bypassing the client is not stopped by any of this — the
 * same caveat the route guards already carry.
 */
@Service
@Transactional(readOnly = true)
public class MaintainerScopeService {

  private final MaintainerScopeDao scopeDao;
  private final UserDao userDao;
  private final DepartmentDao departmentDao;
  private final YearDao yearDao;
  private final SemesterDao semesterDao;
  private final SubjectDao subjectDao;
  private final MaintainerScopeMapper mapper;

  public MaintainerScopeService(
      MaintainerScopeDao scopeDao,
      UserDao userDao,
      DepartmentDao departmentDao,
      YearDao yearDao,
      SemesterDao semesterDao,
      SubjectDao subjectDao,
      MaintainerScopeMapper mapper) {
    this.scopeDao = scopeDao;
    this.userDao = userDao;
    this.departmentDao = departmentDao;
    this.yearDao = yearDao;
    this.semesterDao = semesterDao;
    this.subjectDao = subjectDao;
    this.mapper = mapper;
  }

  public List<MaintainerScopeResponse> findByUser(Integer userId) {
    if (!userDao.existsById(userId)) {
      throw new ResourceNotFoundException("User", userId);
    }
    return scopeDao.findByUserId(userId).stream().map(mapper::toResponse).toList();
  }

  /**
   * Whether a maintainer may change anything filed at an address.
   *
   * <p>Any one grant covering it is enough — grants add up rather than intersect, because they are
   * made to widen access, never to restrict it.
   */
  public boolean mayEdit(Integer userId, Integer departmentId, Integer yearId, Integer semesterId) {
    return scopeDao.findByUserId(userId).stream()
        .anyMatch(scope -> scope.covers(departmentId, yearId, semesterId));
  }

  /** The same question asked about a subject, which is where callers usually stand. */
  public boolean mayEditSubject(Integer userId, Integer subjectId) {
    Subject subject =
        subjectDao
            .findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
    return mayEdit(
        userId,
        subject.getDepartment().getId(),
        subject.getYear().getId(),
        subject.getSemester().getId());
  }

  @Transactional
  public MaintainerScopeResponse grant(GrantScopeRequest request) {
    if (request.semesterId() != null && request.yearId() == null) {
      throw new InvalidRequestException(
          "A semester grant needs the year it belongs to; a semester on its own would cover that"
              + " semester of every year");
    }

    MaintainerScope scope = new MaintainerScope();
    scope.setUser(
        userDao
            .findById(request.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User", request.userId())));
    scope.setDepartment(
        departmentDao
            .findById(request.departmentId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Department", request.departmentId())));
    if (request.yearId() != null) {
      scope.setYear(
          yearDao
              .findById(request.yearId())
              .orElseThrow(() -> new ResourceNotFoundException("Year", request.yearId())));
    }
    if (request.semesterId() != null) {
      scope.setSemester(
          semesterDao
              .findById(request.semesterId())
              .orElseThrow(
                  () -> new ResourceNotFoundException("Semester", request.semesterId())));
    }

    // A grant already covered by a wider one is refused rather than stored. Two rows where one
    // says everything the other does is not extra access, it is a second thing to remember to
    // revoke.
    boolean alreadyCovered =
        scopeDao.findByUserId(request.userId()).stream()
            .anyMatch(
                existing ->
                    existing.covers(request.departmentId(), request.yearId(), request.semesterId()));
    if (alreadyCovered) {
      throw new DuplicateResourceException(
          "That maintainer already has access covering this address");
    }
    return mapper.toResponse(scopeDao.save(scope));
  }

  @Transactional
  public void revoke(Integer scopeId) {
    scopeDao.delete(
        scopeDao
            .findById(scopeId)
            .orElseThrow(() -> new ResourceNotFoundException("Grant", scopeId)));
  }
}
