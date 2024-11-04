// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.DepartmentDao;
import com.kalyan.documan.dao.SemesterDao;
import com.kalyan.documan.dao.SubjectDao;
import com.kalyan.documan.dao.YearDao;
import com.kalyan.documan.entity.*;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubjectService {

  private static final Logger log = LoggerFactory.getLogger(SubjectService.class);
  private final SubjectDao subjectDao;
  private final DepartmentDao departmentDao;
  private final SemesterDao semesterDao;
  private final YearDao yearDao;
  private final RedisCacheService redisCacheService;

  @Autowired
  public SubjectService(
      SubjectDao subjectDao,
      DepartmentDao departmentDao,
      SemesterDao semesterDao,
      YearDao yearDao,
      RedisCacheService redisCacheService) {
    this.subjectDao = subjectDao;
    this.departmentDao = departmentDao;
    this.semesterDao = semesterDao;
    this.yearDao = yearDao;
    this.redisCacheService = redisCacheService;
  }

  public Optional<Subject> getSubjectById(Integer id) {
    String subjectKey = String.format("SUBJECT%s", id);
    Optional<Subject> cachedEntity = redisCacheService.getValue(subjectKey, Subject.class);
    if (cachedEntity.isEmpty()) {
      log.error("Subject {} not found in cache", id);
      Optional<Subject> subject = subjectDao.findById(id);
      if (subject.isPresent()) {
        Optional<Subject> cachedSubject = redisCacheService.setValue(subjectKey, subject.get());
        if (cachedSubject.isEmpty()) {
          log.error("Failed to cache Subject {}", id);
        } else {
          log.info("cached Subject {}", id);
        }
        return subject; // return user from db cache if exists
      } else {
        return Optional.empty();
      }
    } else {
      log.info("Subject {} found in cache", id);
    }
    return cachedEntity;
  }

  public Optional<List<Subject>> getAllSubjects() {
    return Optional.of(subjectDao.findAll());
  }

  public Optional<List<Subject>> getSubjects(
      Integer departmentId, Integer yearId, Integer semesterId) {
    Optional<Department> department = departmentDao.findById(departmentId);
    Optional<Year> year = yearDao.findById(yearId);
    Optional<Semester> semester = semesterDao.findById(semesterId);
    if (department.isPresent() && year.isPresent() && semester.isPresent()) {
      return Optional.of(subjectDao.getSubjects(departmentId, yearId, semesterId));
    }
    return Optional.empty();
  }

  public Optional<Subject> createSubject(
      Subject subject, Integer departmentId, Integer yearId, Integer semesterId) {
    Optional<Department> department = departmentDao.findById(departmentId);
    Optional<Year> year = yearDao.findById(yearId);
    Optional<Semester> semester = semesterDao.findById(semesterId);
    if (department.isPresent()
        && year.isPresent()
        && semester.isPresent()
        && (subject.getId() == null)) {
      Subject newSubject = new Subject();
      newSubject.setDepartment(department.get());
      newSubject.setYear(year.get());
      newSubject.setSemester(semester.get());
      newSubject.setCode(subject.getCode());
      newSubject.setName(subject.getName());
      newSubject.setLab(subject.isLab());
      newSubject.setTheory(subject.isTheory());
      Subject newEntity = subjectDao.save(newSubject);
      String subjectKey = String.format("SUBJECT%s", newEntity.getId());
      Optional<Subject> cachedEntity = redisCacheService.setValue(subjectKey, newEntity);
      if (cachedEntity.isEmpty()) {
        log.error("Failed to cache Subject {}", newEntity.getId());
      } else {
        log.info("Subject {} cached", newEntity.getId());
      }
      return Optional.of(newEntity);
    }
    return Optional.empty();
  }

  public Optional<Subject> updateSubject(
      Subject subject,
      Integer subjectId,
      Integer departmentId,
      Integer yearId,
      Integer semesterId) {
    Optional<Subject> oldSubject = subjectDao.findById(subjectId);
    Optional<Department> department = departmentDao.findById(departmentId);
    Optional<Year> year = yearDao.findById(yearId);
    Optional<Semester> semester = semesterDao.findById(semesterId);
    if (oldSubject.isPresent()
        && department.isPresent()
        && year.isPresent()
        && semester.isPresent()
        && subject.getId() == null) {
      Subject updatedSubject = oldSubject.get();
      updatedSubject.setDepartment(department.get());
      updatedSubject.setYear(year.get());
      updatedSubject.setSemester(semester.get());
      updatedSubject.setCode(subject.getCode());
      updatedSubject.setName(subject.getName());
      updatedSubject.setLab(subject.isLab());
      updatedSubject.setTheory(subject.isTheory());
      Subject updatedEntity = subjectDao.save(updatedSubject);
      String subjectKey = String.format("SUBJECT%s", updatedEntity.getId());
      Optional<Subject> cachedEntity = redisCacheService.updateValue(subjectKey, updatedEntity);
      if (cachedEntity.isEmpty()) {
        log.error("Failed to update Subject {} in cache", updatedEntity.getId());
      } else {
        log.info("Subject {} updated in cache", updatedEntity.getId());
      }
      return Optional.of(updatedEntity);
    }
    return Optional.empty();
  }

  public Optional<Subject> deleteSubject(Integer subjectId) {
    Optional<Subject> subject = subjectDao.findById(subjectId);
    if (subject.isPresent()) {
      Subject deletedSubject = subject.get();
      subjectDao.delete(deletedSubject);
      String subjectKey = String.format("SUBJECT%s", deletedSubject.getId());
      log.info("Subject {} deletion from cache started", deletedSubject.getId());
      redisCacheService.deleteValue(subjectKey);
      log.info("Subject {} deleted from cache ", deletedSubject.getId());
      return Optional.of(deletedSubject);
    }
    return Optional.empty();
  }
}
