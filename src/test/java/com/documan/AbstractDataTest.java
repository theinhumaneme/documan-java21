// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan;

import com.documan.dao.*;
import com.documan.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Boots the full context against the shared PostgreSQL and Redis containers, so the service layer,
 * repository queries and entity mappings run against the database the application actually uses.
 *
 * <p>The object-store client is mocked; storage behaviour is covered separately.
 */
@SpringBootTest
@Import(DatastoreContainers.class)
public abstract class AbstractDataTest {

  @MockitoBean protected S3Client s3Client;

  @Autowired protected UserDao userDao;
  @Autowired protected PostDao postDao;
  @Autowired protected CommentDao commentDao;
  @Autowired protected SubjectDao subjectDao;
  @Autowired protected FileDao fileDao;
  @Autowired protected RoleDao roleDao;
  @Autowired protected YearDao yearDao;
  @Autowired protected SemesterDao semesterDao;
  @Autowired protected DepartmentDao departmentDao;
  @Autowired protected PostVoteDao postVoteDao;
  @Autowired protected CommentVoteDao commentVoteDao;
  @Autowired protected PostFavouriteDao postFavouriteDao;
  @Autowired protected FileFavouriteDao fileFavouriteDao;

  protected Role regularRole;
  protected Department department;
  protected Year year;
  protected Semester semester;

  @Autowired protected JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedReferenceData() {
    jdbcTemplate.update("DELETE FROM search_outbox");
    postVoteDao.deleteAll();
    commentVoteDao.deleteAll();
    postFavouriteDao.deleteAll();
    fileFavouriteDao.deleteAll();
    commentDao.deleteAll();
    postDao.deleteAll();
    fileDao.deleteAll();
    subjectDao.deleteAll();
    userDao.deleteAll();

    regularRole = roleDao.findAll().stream().findFirst().orElseGet(() -> save("regular"));
    department = departmentDao.findAll().stream().findFirst().orElseGet(this::saveDepartment);
    year = yearDao.findAll().stream().findFirst().orElseGet(this::saveYear);
    semester = semesterDao.findAll().stream().findFirst().orElseGet(this::saveSemester);
  }

  private Role save(String name) {
    Role role = new Role();
    role.setName(name);
    return roleDao.save(role);
  }

  private Department saveDepartment() {
    Department entity = new Department();
    entity.setName("Computer Science Engineering");
    return departmentDao.save(entity);
  }

  private Year saveYear() {
    Year entity = new Year();
    entity.setValue("II");
    return yearDao.save(entity);
  }

  private Semester saveSemester() {
    Semester entity = new Semester();
    entity.setName("I");
    return semesterDao.save(entity);
  }

  protected User newUser(String username) {
    User user = new User();
    user.setUsername(username);
    user.setPassword("placeholder-password");
    user.setFirstName("Test");
    user.setLastName("User");
    user.setEmail(username + "@example.test");
    user.setRole(regularRole);
    user.setDepartment(department);
    user.setYear(year);
    user.setSemester(semester);
    return userDao.save(user);
  }

  protected Post newPost(User author, String title) {
    Post post = new Post();
    post.setTitle(title);
    post.setDescription("description");
    post.setContent("content");
    post.setUser(author);
    return postDao.save(post);
  }

  protected Comment newComment(User author, Post post, String content) {
    Comment comment = new Comment();
    comment.setContent(content);
    comment.setUser(author);
    comment.setPost(post);
    return commentDao.save(comment);
  }

  protected Subject newSubject(String name, String code) {
    Subject subject = new Subject();
    subject.setName(name);
    subject.setCode(code);
    subject.setLab(false);
    subject.setTheory(true);
    subject.setDepartment(department);
    subject.setYear(year);
    subject.setSemester(semester);
    return subjectDao.save(subject);
  }

  protected File newFile(Subject subject, String name) {
    File file = new File();
    file.setName(name);
    file.setObjectName("obj-" + name);
    file.setObjectURL("http://localhost/obj-" + name);
    file.setSize(1024L);
    file.setSubject(subject);
    return fileDao.save(file);
  }
}
