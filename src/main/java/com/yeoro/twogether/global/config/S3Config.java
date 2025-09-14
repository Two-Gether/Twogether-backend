package com.yeoro.twogether.global.config;

import com.yeoro.twogether.global.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConfigurationPropertiesScan("com.yeoro.twogether.global.properties")
public class S3Config {

    private final AwsProperties awsProperties;

    private static String t(String s){ return s == null ? null : s.trim(); }

    @Bean
    public S3Client s3Client() {
        String access = t(awsProperties.getCredentials().getAccessKey());
        String secret = t(awsProperties.getCredentials().getSecretKey());
        String region = t(awsProperties.getRegion());

        log.info("[S3Client] region={}, bucket={}, accessKey(prefix)={}",
                region, awsProperties.getS3().getPrivateBucket(),
                access != null && access.length() >= 4 ? access.substring(0,4) : "null");

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(access, secret)))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        String access = t(awsProperties.getCredentials().getAccessKey());
        String secret = t(awsProperties.getCredentials().getSecretKey());
        String region = t(awsProperties.getRegion());
        log.info("[S3Presigner] region={}", region);

        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(access, secret)))
                .build();
    }
}
