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
@ResourceServerTestProperties
@SpringBootTest
@Import(DatastoreContainers.class)
public abstract class AbstractDataTest {

  @MockitoBean protected S3Client s3Client;

  @Autowired protected UserDao userDao;
  @Autowired protected PostDao postDao;
  @Autowired protected CommentDao commentDao;
  @Autowired protected SubjectDao subjectDao;
  @Autowired protected FileDao fileDao;
  @Autowired protected FolderDao folderDao;
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
    // Between files and subjects: a folder holds files and belongs to a subject, so deleting it
    // earlier orphans rows and deleting it later violates the subject's foreign key.
    folderDao.deleteAll();
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

  /**
   * A file in a folder, because there is no other kind.
   *
   * <p>This used to leave {@code folder} null and stopped working when the column became {@code not
   * null} — the fixture fell behind the entity, and every test that filed anything failed on a
   * constraint rather than on what it was testing. A folder is made on demand rather than asked for,
   * so the callers stay about favourites and cascades instead of about scaffolding.
   */
  protected File newFile(Subject subject, String name) {
    File file = new File();
    file.setName(name);
    file.setObjectName("obj-" + name);
    file.setObjectURL("http://localhost/obj-" + name);
    file.setSize(1024L);
    file.setSubject(subject);
    file.setFolder(defaultFolderFor(subject));
    return fileDao.save(file);
  }

  /** One folder per subject, reused, so a test filing several files gets one place to put them. */
  private Folder defaultFolderFor(Subject subject) {
    return folderDao
        .findBySubjectIdAndSlug(subject.getId(), DefaultFolder.COURSEFILES.slug())
        .orElseGet(
            () -> {
              Folder folder = new Folder();
              folder.setName(DefaultFolder.COURSEFILES.displayName());
              folder.setSlug(DefaultFolder.COURSEFILES.slug());
              folder.setDefaultFolder(true);
              folder.setSubject(subject);
              return folderDao.save(folder);
            });
  }
}
