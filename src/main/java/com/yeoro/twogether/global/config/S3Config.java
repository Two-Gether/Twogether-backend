package com.yeoro.twogether.global.config;

import com.yeoro.twogether.global.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@RequiredArgsConstructor
@ConfigurationPropertiesScan("com.yeoro.twogether.global.properties")
public class S3Config {

    private final AwsProperties awsProperties;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            awsProperties.getCredentials().getAccessKey(),
            awsProperties.getCredentials().getSecretKey()
        );

        return S3Client.builder()
            .region(Region.of(awsProperties.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
    }
}
