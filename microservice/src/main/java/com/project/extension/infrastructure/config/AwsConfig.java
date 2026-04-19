package com.project.extension.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("production")
@PropertySource("file:env.properties")
public class AwsConfig {

    @Value("${AWS_ACCESS_KEY}")
    private String accessKey;

    @Value("${AWS_SECRET_KEY}")
    private String secretKey;

    @Value("${AWS_SESSION_TOKEN}")
    private String sessionToken;

    @Value("${AWS_REGION}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(resolveCredentialsProvider())
                .build();
    }

    private AwsCredentialsProvider resolveCredentialsProvider() {
        if (!hasExplicitCredentials()) {
            return DefaultCredentialsProvider.create();
        }
        if (sessionToken != null && !sessionToken.isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(accessKey, secretKey, sessionToken));
        }
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));
    }

    private boolean hasExplicitCredentials() {
        return accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}