package com.yeoro.twogether.domain.place.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.domain.place.dto.request.PlaceCreateRequest;
import com.yeoro.twogether.domain.place.dto.request.PlaceUpdateRequest;
import com.yeoro.twogether.domain.place.dto.response.PlaceByDateResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
        // 현재 로그인한 사용자 정보 조회
        Member member = memberService.getCurrentMember(memberId);

        // metaJson → PlaceCreateRequest DTO로 변환
        PlaceCreateRequest meta = parseCreateMeta(metaJson);

        // 오늘(한국시간 KST) 기준으로 "동일 주소" 하이라이트 존재 여부 검사
        // 오늘 자정(00:00) ~ 내일 자정(00:00) 범위 안에서 같은 주소가 존재하면 예외 발생
        LocalDateTime[] todayRangeKST = todayRangeKST();
        boolean alreadyToday = placeRepository.existsByMemberAndAddressAndCreatedAtBetween(
                member,
                meta.address(),
                todayRangeKST[0],
                todayRangeKST[1]
        );
        if (alreadyToday) {
            // 동일한 장소에 대해 오늘 이미 업로드했다면 다시 올릴 수 없음
            throw new ServiceException(ErrorCode.PLACE_ADDRESS_EXISTS);
        }

        // 태그 검증 (공백 제거, 중복 제거, 최대 5개 제한)
        List<String> tags = validateTags(meta.tags());

        // 이미지 유효성 검사
        if (image == null || image.isEmpty()) {
            throw new ServiceException(ErrorCode.PLACE_CREATION_FAILED);
        }

        // 이미지 업로드 및 롤백 대비 등록
        HighlightS3Service.UploadResult up = uploadImage(memberId, image);
        registerRollbackDelete(up.key()); // 트랜잭션 롤백 시 업로드된 이미지 삭제

        // Place 엔티티 생성 및 저장
        Place place = Place.builder()
                .member(member)
                .imageUrl(up.key())
                .name(meta.name())
                .address(meta.address())
                .description(meta.description())
                .tags(tags)
                .build();

        placeRepository.save(place);

        // Presigned URL 발급 및 응답 반환
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
     * 날짜 기준 Place 조회 (본인 + 연인이 올린 하이라이트 조회)
     */
    @Override
    @Transactional(readOnly = true)
    public PlaceByDateResponse getPlacesByDate(Long memberId, LocalDate dateKst) {
        // 본인/파트너 식별
        Member me = memberService.getCurrentMember(memberId);

        Long partnerId = memberService.getPartnerId(memberId);

        // 날짜(KST) 범위 계산: 해당 날짜 00:00 ~ 다음날 00:00
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate d = (dateKst != null) ? dateKst : LocalDate.now(KST);
        LocalDateTime start = d.atStartOfDay();
        LocalDateTime end = d.plusDays(1).atStartOfDay();

        // 쿼리용 대상 memberIds
        List<Long> memberIds = (partnerId != null)
                ? List.of(memberId, partnerId)
                : List.of(memberId);

        // 조회
        List<Place> all = placeRepository.findAllByMember_IdInAndCreatedAtBetween(memberIds, start, end);

        // presigned URL 변환
        var mine = all.stream()
                .filter(p -> p.getMember().getId().equals(memberId))
                .map(p -> {
                    String key = p.getImageUrl();
                    String url = (key == null || key.isBlank()) ? null : highlightS3Service.presignedGetUrl(key);
                    return PlaceResponse.fromWithResolvedUrl(p, url);
                })
                .toList();

        var partner = (partnerId == null) ? List.<PlaceResponse>of()
                : all.stream()
                .filter(p -> p.getMember().getId().equals(partnerId))
                .map(p -> {
                    String key = p.getImageUrl();
                    String url = (key == null || key.isBlank()) ? null : highlightS3Service.presignedGetUrl(key);
                    return PlaceResponse.fromWithResolvedUrl(p, url);
                })
                .toList();

        return new PlaceByDateResponse(mine, partner);
    }

    // ---------------------------------------------------------------------------------------------------------------



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
     * 태그 검증 (최대 5개)
     */
    private List<String> validateTags(List<String> tags) {
        List<String> safe = Optional.ofNullable(tags).orElse(List.of()).stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .distinct()
                .limit(5) // 과다 요청 방지
                .toList();
        if (safe.size() > 5) {
            throw new ServiceException(ErrorCode.PLACE_TAG_LIMIT_EXCEEDED);
        }
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



    private LocalDateTime[] todayRangeKST() {
        ZoneId KST = ZoneId.of("Asia/Seoul"); // 한국 시간 기준
        LocalDate today = LocalDate.now(KST);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return new LocalDateTime[]{ start, end };
    }
}