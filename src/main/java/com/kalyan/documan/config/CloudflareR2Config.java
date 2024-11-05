// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

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
    AwsBasicCredentials awsCredentials =
        AwsBasicCredentials.create(cloudflareR2AccessKeyId, cloudflareR2SecretAccessKey);

    S3Configuration s3Configuration =
        S3Configuration.builder().pathStyleAccessEnabled(true).build();

    return S3Client.builder()
        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
        .endpointOverride(URI.create(cloudflareR2Endpoint))
        .serviceConfiguration(s3Configuration)
        .region(Region.of("auto"))
        .build();
  }
}
