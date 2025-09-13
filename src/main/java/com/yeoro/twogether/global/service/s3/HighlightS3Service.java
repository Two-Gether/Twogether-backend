package com.yeoro.twogether.global.service.s3;

import com.yeoro.twogether.global.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
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
public class HighlightS3Service {

    private final S3Client s3;
    private final S3Presigner presigner;   // Presigner 주입
    private final AwsProperties aws;

    // presign TTL
    private static final Duration PRESIGN_TTL = Duration.ofHours(3);

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final String BASE_PREFIX = "highlights/";

    public record UploadResult(String key, String sha256) {}

    /** 업로드: 반드시 byte[] 경로만 사용 */
    public UploadResult upload(Long memberId,
                               String originalFileName,
                               String contentType,
                               byte[] bytes) {

        // 1) 키 생성
        String ym = LocalDate.now().format(YM);
        String ext = extOf(originalFileName);
        String key = BASE_PREFIX + memberId + "/" + ym + "/" + UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);

        // 2) Content-Type 보정
        String ct = (contentType == null || contentType.isBlank())
                ? guessContentType(originalFileName)
                : contentType;

        // 3) SHA-256 계산 → 메타에 심기
        String sha256 = sha256Hex(bytes);

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(aws.getS3().getPrivateBucket())
                .key(key)
                .contentType(ct)
                .metadata(Map.of(
                        "sha256", sha256
                ))
                .build();

        s3.putObject(put, RequestBody.fromBytes(bytes));

        return new UploadResult(key, sha256);
    }

    /** presigned GET URL (3시간) */
    public String presignedGetUrl(String key) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(aws.getS3().getPrivateBucket())
                .key(key)
                .build();

        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGN_TTL)
                .getObjectRequest(get)
                .build();

        return presigner.presignGetObject(presign)
                .url()
                .toString();
    }

    /** 기존 객체의 sha256 메타 조회 */
    public String headSha256(String key) {
        HeadObjectRequest req = HeadObjectRequest.builder()
                .bucket(aws.getS3().getPrivateBucket())
                .key(key)
                .build();
        HeadObjectResponse res = s3.headObject(req);
        Map<String, String> meta = res.metadata();
        return meta == null ? null : meta.get("sha256");
    }

    /** 객체 삭제 */
    public void delete(String key) {
        s3.deleteObject(b -> b.bucket(aws.getS3().getPrivateBucket()).key(key));
    }

    // ========= utils =========

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

    public static String sha256Hex(byte[] bytes) {
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

    public void deleteQuietly(String key) {
        if (key == null || key.isBlank()) return;
        try {
            delete(key);
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            log.warn("[S3] deleteQuietly no-such-key: {}", key);
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            log.warn("[S3] deleteQuietly s3-exception: key={}, msg={}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("[S3] deleteQuietly unexpected: key={}", key, e);
        }
    }

}