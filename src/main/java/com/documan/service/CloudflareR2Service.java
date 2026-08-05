// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.exception.StorageException;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Thin gateway over the object store. Business rules and database writes live in {@link
 * FileService}; this class only moves bytes and reports failures as {@link StorageException}.
 *
 * <p>Uploads stream straight from the multipart part to R2. The previous implementation copied
 * every upload into a file in the process working directory, named after the client-supplied
 * filename, which meant concurrent uploads of the same name clobbered one another and a failure
 * between write and delete leaked the file.
 */
@Service
public class CloudflareR2Service {

  private static final Logger log = LoggerFactory.getLogger(CloudflareR2Service.class);

  private final S3Client s3Client;

  @Value("${cloudflare.r2.files-bucket}")
  private String filesBucket;

  @Value("${cloudflare.r2.user-bucket}")
  private String userBucket;

  /**
   * R2 does not implement S3 ACLs; public exposure is configured on the bucket or via a custom
   * domain. Left switchable so existing buckets that expect the canned ACL keep working.
   */
  @Value("${cloudflare.r2.public-read-acl:true}")
  private boolean publicReadAcl;

  public CloudflareR2Service(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  public void uploadFile(String objectName, MultipartFile file) {
    PutObjectRequest.Builder request =
        PutObjectRequest.builder()
            .bucket(filesBucket)
            .key(objectName)
            .contentType(file.getContentType())
            .contentLength(file.getSize());
    if (publicReadAcl) {
      request.acl(ObjectCannedACL.PUBLIC_READ);
    }

    try {
      s3Client.putObject(
          request.build(), RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    } catch (IOException | S3Exception e) {
      throw new StorageException("Failed to upload object '%s'".formatted(objectName), e);
    }
  }

  /**
   * @return {@code true} if the object existed and was removed.
   */
  public boolean deleteFile(String objectName) {
    if (!objectExists(filesBucket, objectName)) {
      return false;
    }
    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder().bucket(filesBucket).key(objectName).build());
      return true;
    } catch (S3Exception e) {
      throw new StorageException("Failed to delete object '%s'".formatted(objectName), e);
    }
  }

  public boolean objectExists(String bucket, String key) {
    try {
      s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    } catch (S3Exception e) {
      if (e.statusCode() == 404) {
        return false;
      }
      throw new StorageException("Failed to stat object '%s'".formatted(key), e);
    }
  }

  public PutObjectResponse uploadProfilePicture(String key, Path filePath) {
    try {
      return s3Client.putObject(
          PutObjectRequest.builder().bucket(userBucket).key(key).build(), filePath);
    } catch (S3Exception e) {
      throw new StorageException("Failed to upload profile picture '%s'".formatted(key), e);
    }
  }

  /** Best-effort cleanup used to avoid orphaning an object when the metadata write fails. */
  void deleteQuietly(String objectName) {
    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder().bucket(filesBucket).key(objectName).build());
    } catch (RuntimeException e) {
      log.error("Failed to clean up orphaned object '{}'", objectName, e);
    }
  }
}
