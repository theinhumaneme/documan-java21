// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class CloudflareR2Config {

  @Value("${cloudflare.r2.access-key-id}")
  private String cloudflareR2AccessKeyId;

  @Value("${cloudflare.r2.secret-access-key}")
  private String cloudflareR2SecretAccessKey;

  @Value("${cloudflare.r2.endpoint}")
  private String cloudflareR2Endpoint;

  @Bean
  public S3Client s3Client() {
    S3Configuration s3Configuration =
        S3Configuration.builder().pathStyleAccessEnabled(true).build();

    return S3Client.builder()
        .credentialsProvider(credentials())
        .endpointOverride(URI.create(cloudflareR2Endpoint))
        .serviceConfiguration(s3Configuration)
        .region(Region.of("auto"))
        .build();
  }

  /**
   * Signs download URLs.
   *
   * <p>Needed because the bucket's public URL cannot be made to trigger a download. R2 serves an
   * object with its content type and no {@code Content-Disposition}, so a browser renders a PDF or
   * an image inline, and the {@code download} attribute on a link is ignored cross-origin.
   * Presigning lets the disposition and the filename be set per request without changing how the
   * object is stored, and the download still comes from Cloudflare's edge rather than through this
   * service.
   */
  @Bean
  public S3Presigner s3Presigner() {
    return S3Presigner.builder()
        .credentialsProvider(credentials())
        .endpointOverride(URI.create(cloudflareR2Endpoint))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .region(Region.of("auto"))
        .build();
  }

  private StaticCredentialsProvider credentials() {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(cloudflareR2AccessKeyId, cloudflareR2SecretAccessKey));
  }
}
