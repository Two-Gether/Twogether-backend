package com.yeoro.twogether.global.service.s3;

import com.yeoro.twogether.global.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URLConnection;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileS3Service {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final AwsProperties aws;

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy/MM");

    public record UploadResult(String key, String sha256) {}

    public UploadResult upload(Long memberId, String originalFileName, String contentType, byte[] bytes) {
        String ym = LocalDate.now().format(YM);
        String ext = extOf(originalFileName);
        String base = aws.getS3().profilePrefix(); // "profile/"
        String key = base + memberId + "/" + ym + "/" + UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);

        String ct = (contentType == null || contentType.isBlank())
                ? guessContentType(originalFileName)
                : contentType;

        String sha256 = sha256Hex(bytes);

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(aws.getS3().getPrivateBucket())
                .key(key)
                .contentType(ct)
                .metadata(Map.of("sha256", sha256))
                .build();

        s3.putObject(put, RequestBody.fromBytes(bytes));
        return new UploadResult(key, sha256);
    }

    public String presignedGetUrl(String key) {
        if (key == null || key.isBlank()) return null;
        int ttl = aws.getS3().presignTtlSecondsOrDefault(); // yml: 10800(=3시간)
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(aws.getS3().getPrivateBucket())
                .key(key)
                .build();
        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(ttl))
                .getObjectRequest(get)
                .build();
        return presigner.presignGetObject(presign).url().toString();
    }

    public String headSha256(String key) {
        HeadObjectResponse res = s3.headObject(b -> b
                .bucket(aws.getS3().getPrivateBucket())
                .key(key));
        Map<String, String> meta = res.metadata();
        return meta == null ? null : meta.get("sha256");
    }

    public void deleteQuietly(String key) {
        if (key == null || key.isBlank()) return;
        try {
            s3.deleteObject(b -> b.bucket(aws.getS3().getPrivateBucket()).key(key));
        } catch (NoSuchKeyException e) {
            log.warn("[S3] deleteQuietly no-such-key: {}", key);
        } catch (S3Exception e) {
            log.warn("[S3] deleteQuietly s3-exception: key={}, msg={}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("[S3] deleteQuietly unexpected: key={}", key, e);
        }
    }

    // utils
    private static String extOf(String fileName) {
        if (fileName == null) return "";
        int i = fileName.lastIndexOf('.');
        if (i < 0 || i == fileName.length() - 1) return "";
        return fileName.substring(i + 1).replaceAll("[^A-Za-z0-9]", "");
    }
    private static String guessContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String ct = URLConnection.guessContentTypeFromName(fileName);
        return (ct == null) ? "application/octet-stream" : ct;
    }
    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(bytes);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
