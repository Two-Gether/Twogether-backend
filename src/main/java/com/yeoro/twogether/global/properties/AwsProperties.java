package com.yeoro.twogether.global.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "cloud.aws")
public class AwsProperties {

    private final Credentials credentials;
    private final String region;
    private final S3 s3;

    @Getter
    @RequiredArgsConstructor
    public static class Credentials {

        private final String accessKey;
        private final String secretKey;
    }

    @Getter
    @RequiredArgsConstructor
    public static class S3 {

        private final String publicBucket;
        private final String privateBucket;
    }
}
