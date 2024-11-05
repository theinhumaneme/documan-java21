// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.FileDao;
import com.kalyan.documan.dao.SubjectDao;
import com.kalyan.documan.entity.Subject;
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
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

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

  public Optional<com.kalyan.documan.entity.File> uploadFile(
      MultipartFile file, Integer subjectId) {
    try {
      Optional<Subject> subject = subjectDao.findById(subjectId);
      if (subject.isEmpty() && file.getOriginalFilename() != null) {
        return Optional.empty();
      } else {
        com.kalyan.documan.entity.File fileEntity = new com.kalyan.documan.entity.File();
        StringBuilder fileName = new StringBuilder();
        StringBuilder fileURL = new StringBuilder();
        fileName
            .append(file.getOriginalFilename().replace(" ", "_"))
            .append("_")
            .append(UUID.randomUUID());
        fileEntity.setName(file.getOriginalFilename().toString());
        fileEntity.setSize(file.getSize());
        fileEntity.setSubject(subject.get());
        try {
          PutObjectRequest uploadFileRequest =
              PutObjectRequest.builder()
                  .bucket(documanFilesBucketName)
                  .acl(ObjectCannedACL.PUBLIC_READ)
                  .key(fileName.toString())
                  .contentType(file.getContentType())
                  .build();
          File convertedFile = convertMultiPartToFile(file);
          s3Client.putObject(uploadFileRequest, RequestBody.fromFile(convertedFile));
          convertedFile.delete();
        } catch (Exception e) {
          log.error(e.toString());
          return Optional.empty();
        }
        fileURL.append(documanFilesPublicAccessUrl).append("/").append(fileName);
        fileEntity.setObjectName(fileName.toString());
        fileEntity.setObjectURL(fileURL.toString());
        fileDao.save(fileEntity);

        return Optional.of(fileEntity);
      }
    } catch (Exception e) {
      log.error(e.toString());
    }
    return Optional.empty();
  }

  private File convertMultiPartToFile(MultipartFile file) throws IOException {
    File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
    FileOutputStream FileStream = new FileOutputStream(convFile);
    FileStream.write(file.getBytes());
    FileStream.close();
    return convFile;
  }

  public PutObjectResponse uploadProfilePicture(String key, Path filePath) {
    PutObjectRequest request =
        PutObjectRequest.builder().bucket(documanUserDataBucketName).key(key).build();

    return s3Client.putObject(request, filePath);
  }
}
