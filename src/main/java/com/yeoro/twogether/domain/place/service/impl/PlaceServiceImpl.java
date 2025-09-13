package com.yeoro.twogether.domain.place.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.domain.place.dto.request.PlaceCreateRequest;
import com.yeoro.twogether.domain.place.dto.request.PlaceUpdateRequest;
import com.yeoro.twogether.domain.place.dto.response.PlaceCreateResponse;
import com.yeoro.twogether.domain.place.dto.response.PlaceResponse;
import com.yeoro.twogether.domain.place.entity.Place;
import com.yeoro.twogether.domain.place.repository.PlaceRepository;
import com.yeoro.twogether.domain.place.service.PlaceService;
import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import com.yeoro.twogether.global.service.s3.HighlightS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService {

    private final PlaceRepository placeRepository;
    private final MemberService memberService;
    private final HighlightS3Service highlightS3Service;
    private final ObjectMapper objectMapper;

    /**
     * Place 생성
     */
    @Override
    @Transactional
    public PlaceCreateResponse createPlace(Long memberId, String metaJson, MultipartFile image) {
        Member member = memberService.getCurrentMember(memberId);
        PlaceCreateRequest meta = parseCreateMeta(metaJson);

        if (placeRepository.existsByMemberAndAddress(member, meta.address())) {
            throw new ServiceException(ErrorCode.PLACE_ADDRESS_EXISTS);
        }

        List<String> tags = validateTags(meta.tags());

        if (image == null || image.isEmpty()) {
            throw new ServiceException(ErrorCode.PLACE_CREATION_FAILED);
        }

        // 업로드 키를 미리 받아두고, 롤백 시 정리
        HighlightS3Service.UploadResult up = uploadImage(memberId, image);
        registerRollbackDelete(up.key());

        Place place = Place.builder()
                .member(member)
                .imageUrl(up.key())
                .name(meta.name())
                .address(meta.address())
                .description(meta.description())
                .tags(tags)
                .build();

        placeRepository.save(place);

        String presigned = highlightS3Service.presignedGetUrl(up.key());
        return PlaceCreateResponse.fromWithResolvedUrl(place, presigned);
    }


    /**
     * 주소로 Place 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<PlaceResponse> getPlace(String address) {
        return placeRepository.findAllByAddress(address).stream()
                .map(p -> {
                    String key = p.getImageUrl();
                    String presigned = (key == null || key.isBlank())
                            ? null
                            : highlightS3Service.presignedGetUrl(key);
                    return PlaceResponse.fromWithResolvedUrl(p, presigned);
                })
                .toList();
    }

    /**
     * ID로 Place 단건 조회
     */
    @Override
    @Transactional(readOnly = true)
    public PlaceResponse getOnePlace(Long memberId, Long placeId) {
        Place place = placeRepository.findByIdAndMember_Id(placeId, memberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PLACE_NOT_FOUND));

        String key = place.getImageUrl();
        String presigned = (key == null || key.isBlank())
                ? null
                : highlightS3Service.presignedGetUrl(key);

        return PlaceResponse.fromWithResolvedUrl(place, presigned);
    }

    /**
     * Place 수정
     */
    @Override
    @Transactional
    public PlaceResponse updatePlace(Long memberId, Long placeId, String metaJson, MultipartFile image) {
        PlaceUpdateRequest request = parseUpdateMeta(metaJson);
        Place place = placeRepository.findByIdAndMember_Id(placeId, memberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PLACE_NOT_FOUND));

        List<String> tags = validateTags(request.tags());

        String currentKey = place.getImageUrl();
        String finalKey = processImageUpdate(memberId, image, currentKey);

        place.overwrite(
                finalKey,
                request.name(),
                request.description(),
                tags
        );

        String presigned = (finalKey == null || finalKey.isBlank())
                ? null
                : highlightS3Service.presignedGetUrl(finalKey);
        return PlaceResponse.fromWithResolvedUrl(place, presigned);
    }

    /**
     * Place 삭제
     */
    @Override
    @Transactional
    public void deletePlace(Long memberId, Long placeId) {
        Place place = placeRepository.findByIdAndMember_Id(placeId, memberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PLACE_NOT_FOUND));

        String keyToDelete = place.getImageUrl();

        placeRepository.delete(place);

        if (keyToDelete != null && !keyToDelete.isBlank()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    highlightS3Service.deleteQuietly(keyToDelete);
                }
            });
        }
    }



    /**
     * Create 요청 JSON 파싱
     */
    private PlaceCreateRequest parseCreateMeta(String metaJson) {
        try {
            return objectMapper.readValue(metaJson, PlaceCreateRequest.class);
        } catch (IOException e) {
            throw new ServiceException(ErrorCode.PLACE_CREATION_FAILED);
        }
    }

    /**
     * Update 요청 JSON 파싱
     */
    private PlaceUpdateRequest parseUpdateMeta(String metaJson) {
        try {
            return objectMapper.readValue(metaJson, PlaceUpdateRequest.class);
        } catch (IOException e) {
            throw new ServiceException(ErrorCode.PLACE_CREATION_FAILED);
        }
    }

    /**
     * 태그 검증 (최대 2개)
     */
    private List<String> validateTags(List<String> tags) {
        List<String> safe = Optional.ofNullable(tags).orElse(List.of()).stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (safe.size() > 2) throw new ServiceException(ErrorCode.PLACE_TAG_LIMIT_EXCEEDED);
        return safe;
    }

    /**
     * 이미지 업로드
     */
    private HighlightS3Service.UploadResult uploadImage(Long memberId, MultipartFile image) {
        try {
            byte[] bytes = image.getBytes();
            return highlightS3Service.upload(
                    memberId,
                    image.getOriginalFilename(),
                    image.getContentType(),
                    bytes
            );
        } catch (IOException e) {
            throw new ServiceException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 이미지 교체 처리
     */
    private String processImageUpdate(Long memberId, MultipartFile image, String currentKey) {
        if (image == null || image.isEmpty()) return currentKey;

        try {
            byte[] bytes = image.getBytes();
            String newSha = sha256Hex(bytes);

            boolean same = false;
            if (currentKey != null && !currentKey.isBlank()) {
                try {
                    String oldSha = highlightS3Service.headSha256(currentKey);
                    same = (oldSha != null && oldSha.equalsIgnoreCase(newSha));
                } catch (S3Exception e) {
                    log.warn("[processImageUpdate] 기존 sha 조회 실패 → 새 업로드 진행: {}", currentKey);
                }
            }
            if (same) return currentKey;

            var up = highlightS3Service.upload(
                    memberId,
                    image.getOriginalFilename(),
                    image.getContentType(),
                    bytes
            );
            String newKey = up.key();

            // 트랜잭션 롤백 시 새로 올린 객체도 정리
            registerRollbackDelete(newKey);
            // 커밋 후 기존 객체 삭제는 조용히
            deleteOldImageAfterCommit(currentKey, newKey);

            return newKey;
        } catch (IOException e) {
            throw new ServiceException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }


    /**
     * 트랜잭션 커밋 이후 기존 이미지 삭제
     */
    private void deleteOldImageAfterCommit(String oldKey, String newKey) {
        if (oldKey == null || oldKey.isBlank() || oldKey.equals(newKey)) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                highlightS3Service.deleteQuietly(oldKey);
            }
        });
    }

    /**
     * SHA-256 해시 계산
     */
    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(bytes);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 트랜잭션 롤백 시 새로 올린 키 정리
     */
    private void registerRollbackDelete(String key) {
        if (key == null || key.isBlank()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                // STATUS_ROLLED_BACK == 1
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    highlightS3Service.deleteQuietly(key);
                    log.warn("[S3] rolled back, deleted orphan upload: {}", key);
                }
            }
        });
    }
}