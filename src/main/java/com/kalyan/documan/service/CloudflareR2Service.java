// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Service
public class CloudflareR2Service {

  private final S3Client s3Client;

  @Value("${cloudflare.r2.files-bucket}")
  private static String documanFilesBucketName;

  @Value("${cloudflare.r2.user-bucket}")
  private static String documanUserDataBucketName;

  public CloudflareR2Service(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  public PutObjectResponse uploadFile(String key, Path filePath) {
    PutObjectRequest request =
        PutObjectRequest.builder().bucket(documanFilesBucketName).key(key).build();

    return s3Client.putObject(request, filePath);
  }

  public PutObjectResponse uploadProfilePicture(String key, Path filePath) {
    PutObjectRequest request =
        PutObjectRequest.builder().bucket(documanUserDataBucketName).key(key).build();

    return s3Client.putObject(request, filePath);
  }
}
