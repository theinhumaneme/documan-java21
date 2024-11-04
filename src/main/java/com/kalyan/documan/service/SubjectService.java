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
import com.kalyan.documan.entity.Department;
import com.kalyan.documan.entity.Semester;
import com.kalyan.documan.entity.Subject;
import com.kalyan.documan.entity.Year;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubjectService {

  private final SubjectDao subjectDao;
  private final DepartmentDao departmentDao;
  private final SemesterDao semesterDao;
  private final YearDao yearDao;

  @Autowired
  public SubjectService(
      SubjectDao subjectDao,
      DepartmentDao departmentDao,
      SemesterDao semesterDao,
      YearDao yearDao) {
    this.subjectDao = subjectDao;
    this.departmentDao = departmentDao;
    this.semesterDao = semesterDao;
    this.yearDao = yearDao;
  }

  public Optional<List<Subject>> getAllSubjects() {
    return Optional.of(subjectDao.findAll());
  }

  public Optional<Subject> getSubjectById(Integer id) {
    return subjectDao.findById(id);
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
      return Optional.of(subjectDao.save(newSubject));
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
      return Optional.of(subjectDao.save(updatedSubject));
    }
    return Optional.empty();
  }

  public Optional<Subject> deleteSubject(Integer subjectId) {
    Optional<Subject> subject = subjectDao.findById(subjectId);
    if (subject.isPresent()) {
      Subject deletedSubject = subject.get();
      subjectDao.delete(deletedSubject);
      return subject;
    }
    return Optional.empty();
  }
}
