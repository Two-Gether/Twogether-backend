package com.yeoro.twogether.global.service.s3;

import com.yeoro.twogether.global.properties.AwsProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

@Service
@RequiredArgsConstructor
public class StickerS3Service {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    public List<String> listStickerUrls(String dirName) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
            .bucket(awsProperties.getS3().getPublicBucket())
            .prefix(dirName + "/")
            .build();

        ListObjectsV2Response result = s3Client.listObjectsV2(request);

        return result.contents().stream()
            .filter(obj -> !obj.key().endsWith("/")) // 폴더는 제외
            .map(s3Object -> String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                awsProperties.getS3().getPublicBucket(),
                s3Client.serviceClientConfiguration().region().id(),
                s3Object.key()
            ))
            .toList();
    }
}
