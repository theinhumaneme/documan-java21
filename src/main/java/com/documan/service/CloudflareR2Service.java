// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.dao.FileDao;
import com.documan.dao.SubjectDao;
import com.documan.entity.Subject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class CloudflareR2Service {

  private static final Logger log = LoggerFactory.getLogger(CloudflareR2Service.class);
  private final S3Client s3Client;
  private final SubjectDao subjectDao;
  private final FileDao fileDao;

  @Value("${cloudflare.r2.files-bucket}")
  private String documanFilesBucketName;

  @Value("${cloudflare.r2.user-bucket}")
  private String documanUserDataBucketName;

  @Value("${cloudflare.r2.files-bucket-public-access-url}")
  private String documanFilesPublicAccessUrl;

  @Autowired
  public CloudflareR2Service(S3Client s3Client, SubjectDao subjectDao, FileDao fileDao) {
    this.s3Client = s3Client;
    this.subjectDao = subjectDao;
    this.fileDao = fileDao;
  }

  public Optional<com.documan.entity.File> uploadFile(MultipartFile file, Integer subjectId) {
    try {
      Optional<Subject> subject = subjectDao.findById(subjectId);
      if (subject.isEmpty() && file.getOriginalFilename() != null) {
        return Optional.empty();
      } else {
        StringBuilder fileName = new StringBuilder();
        fileName
            .append(UUID.randomUUID().toString().replace("-", ""))
            .append("_")
            .append(file.getOriginalFilename().replace(" ", "-"));

        if (uploadR2Object(documanFilesBucketName, fileName.toString(), file)) {
          com.documan.entity.File fileEntity = new com.documan.entity.File();
          StringBuilder fileURL = new StringBuilder();
          fileEntity.setName(file.getOriginalFilename().toString());
          fileEntity.setSize(file.getSize());
          fileEntity.setSubject(subject.get());
          fileURL.append(documanFilesPublicAccessUrl).append("/").append(fileName);
          fileEntity.setObjectName(fileName.toString());
          fileEntity.setObjectURL(fileURL.toString());
          fileDao.save(fileEntity);
          return Optional.of(fileEntity);
        }
        // return empty if file cant be uploaded
        return Optional.empty();
      }
    } catch (Exception e) {
      log.error(e.toString());
    }
    return Optional.empty();
  }

  public Optional<String> deleteFile(String objectUID) {
    try {
      if (deleteR2Object(documanFilesBucketName, objectUID)) {
        return Optional.of("File deleted successfully");
      }
      return Optional.empty();
    } catch (Exception e) {
      log.error(e.toString());
      return Optional.empty();
    }
  }

  private boolean uploadR2Object(String bucketName, String fileName, MultipartFile file) {
    try {
      PutObjectRequest uploadFileRequest =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .acl(ObjectCannedACL.PUBLIC_READ)
              .key(fileName)
              .contentType(file.getContentType())
              .build();
      Optional<File> convertedFileOptional = convertMultiPartToFile(file);
      if (convertedFileOptional.isPresent()) {
        File convertedFile = convertedFileOptional.get();
        if (s3Client.putObject(uploadFileRequest, RequestBody.fromFile(convertedFile)) != null) {
          convertedFile.delete();
          return true;
        }
        // if the status of the put operation is null, uploading object failed
        return false;
      }
      // Multipart File conversion failed
      return false;
    } catch (Exception e) {
      log.error(e.toString());
      return false;
    }
  }

  public boolean deleteR2Object(String bucketName, String objectUID) {

    try {
      if (objectExists(bucketName, objectUID)) {
        DeleteObjectRequest deleteObjectRequest =
            DeleteObjectRequest.builder().bucket(bucketName).key(objectUID).build();
        s3Client.deleteObject(deleteObjectRequest);
        return true;
      } else {
        return false;
      }

    } catch (Exception e) {
      log.error(e.toString());
      return false;
    }
  }

  public boolean objectExists(String bucket, String key) {
    try {
      HeadObjectResponse headResponse =
          s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    }
  }

  private Optional<File> convertMultiPartToFile(MultipartFile file) throws IOException {
    File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
    try (FileOutputStream fileStream = new FileOutputStream(convFile)) {
      fileStream.write(file.getBytes());
      return Optional.of(convFile);
    } catch (Exception e) {
      log.error(e.toString());
    }
    return Optional.empty();
  }

  public PutObjectResponse uploadProfilePicture(String key, Path filePath) {
    PutObjectRequest request =
        PutObjectRequest.builder().bucket(documanUserDataBucketName).key(key).build();

    return s3Client.putObject(request, filePath);
  }
}
