// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.service;

import com.documan.exception.StorageException;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

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
  private final S3Presigner s3Presigner;

  @Value("${cloudflare.r2.files-bucket}")
  private String filesBucket;

  /**
   * R2 does not implement S3 ACLs; public exposure is configured on the bucket or via a custom
   * domain. Left switchable so existing buckets that expect the canned ACL keep working.
   */
  @Value("${cloudflare.r2.public-read-acl:true}")
  private boolean publicReadAcl;

  public CloudflareR2Service(S3Client s3Client, S3Presigner s3Presigner) {
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
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

  /**
   * A short-lived URL that downloads the object under {@code filename} instead of displaying it.
   *
   * <p>The bucket's public URL cannot do this. R2 returns an object with its content type and no
   * {@code Content-Disposition}, so a browser renders a PDF or an image inline, and a link's {@code
   * download} attribute is ignored because the bucket is a different origin. Presigning carries
   * both the disposition and the name as signed query parameters, so nothing about how the object
   * is stored has to change and the bytes still come from Cloudflare's edge rather than through
   * this service.
   *
   * <p>The name is quoted and stripped of quotes and control characters: it is client-supplied and
   * goes into a response header, where an unescaped quote would let it inject header content.
   */
  public String presignedDownloadUrl(String objectName, String filename, Duration validFor) {
    String safe = filename.replaceAll("[\"\\\\\\r\\n]", "_");
    GetObjectRequest get =
        GetObjectRequest.builder()
            .bucket(filesBucket)
            .key(objectName)
            .responseContentDisposition("attachment; filename=\"%s\"".formatted(safe))
            .build();
    try {
      return s3Presigner
          .presignGetObject(
              GetObjectPresignRequest.builder()
                  .signatureDuration(validFor)
                  .getObjectRequest(get)
                  .build())
          .url()
          .toString();
    } catch (RuntimeException e) {
      throw new StorageException("Failed to sign a download URL for '%s'".formatted(objectName), e);
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
