package com.yeoro.twogether.domain.place.service.impl;

import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.domain.place.dto.request.PlaceCreateRequest;
import com.yeoro.twogether.domain.place.dto.request.PlaceUpdateRequest;
import com.yeoro.twogether.domain.place.dto.response.PlaceCreateResponse;
import com.yeoro.twogether.domain.place.dto.response.PlaceOneSearchRequest;
import com.yeoro.twogether.domain.place.dto.response.PlaceResponse;
import com.yeoro.twogether.domain.place.entity.Place;
import com.yeoro.twogether.domain.place.repository.PlaceRepository;
import com.yeoro.twogether.domain.place.service.PlaceService;
import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService {
    private final PlaceRepository placeRepository;
    private final MemberService memberService;

    /**
     * Place(하이라이트) 생성
     * @param memberId 사용자
     * @param request Place(하이라이트) 생성 DTO
     * @return 저장한 Place객체 반환
     */
    @Override
    @Transactional
    public PlaceCreateResponse createPlace(Long memberId, PlaceCreateRequest request) {

        Member member = memberService.getCurrentMember(memberId);

        // 동일한 주소에 대한 Place 객체가 존재하는지 확인
        if (placeRepository.existsByMemberAndAddress(member, request.address())) {
            throw new ServiceException(ErrorCode.PLACE_ADDRESS_EXISTS);
        }

        // 태그 개수 확인
        List<String> tags = Optional.ofNullable(request.tags()).orElseGet(List::of);
        if (tags.size() > 2) {
            throw new ServiceException(ErrorCode.PLACE_TAG_LIMIT_EXCEEDED);
        }

        // Place 엔티티 생성
        Place place = Place.builder()
                .member(member)
                .imageUrl(request.imageUrl())
                .name(request.name())
                .address(request.address())
                .description(request.description())
                .tags(tags)
                .build();
        placeRepository.save(place);

        // 응답 DTO 생성
        return PlaceCreateResponse.from(place);
    }

    /**
     * 주소에 대한 Place(하이라이트) 객체 리스트 반환
     * @param address 검색할 주소
     * @return 검색된 Place(하이라이트) 객체들의 리스트
     */
    @Override
    @Transactional(readOnly = true)
    public List<PlaceResponse> getPlace(String address) {
        List<Place> places = placeRepository.findAllByAddress(address);
        return PlaceResponse.fromList(places); // 빈 리스트면 [] 반환
    }

    /**
     * 주소에 대한 Place(하이라이트) 단건 조회
     * @param memberId 사용자
     * @param request 조회할 주소
     * @return 조회된 Place(하이라이트) 객체
     */
    @Override
    @Transactional(readOnly = true)
    public PlaceResponse getOnePlace(Long memberId, PlaceOneSearchRequest request) {
        Place place = placeRepository.findByMember_IdAndAddress(memberId, request.address())
                .orElseThrow(() -> new ServiceException(ErrorCode.PLACE_NOT_FOUND));
        return PlaceResponse.from(place);
    }

    /**
     * Place(하이라이트) 객체 수정
     * @param memberId 사용자
     * @param request 변경할 내용
     * @return 변경된 Place(하이라이트)객체
     */
    @Override
    @Transactional
    public PlaceResponse updatePlace(Long memberId, PlaceUpdateRequest request) {
        // 대상 엔티티 조회 (내가 등록한 address여야 함)
        Place place = placeRepository.findByMember_IdAndAddress(memberId, request.address())
                .orElseThrow(() -> new ServiceException(ErrorCode.PLACE_NOT_FOUND));

        // 태그 개수만 확인
        List<String> tags = Optional.ofNullable(request.tags()).orElse(List.of());
        if (tags.size() > 2) {
            throw new ServiceException(ErrorCode.PLACE_TAG_LIMIT_EXCEEDED);
        }

        // 그대로 덮어쓰기 (address는 변경하지 않음)
        place.overwrite(
                request.imageUrl(),
                request.name(),
                request.description(),
                tags
        );

        // 저장 (변경감지로 flush)
        return PlaceResponse.from(place);
    }

    /**
     * Place(하이라이트) 객체 삭제
     * @param memberId 사용자
     * @param placeId 삭제할 하이라이트 객체
     */
    @Override
    @Transactional
    public void deletePlace(Long memberId, Long placeId) {
        Place place = placeRepository.findByIdAndMember_Id(placeId, memberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PLACE_NOT_FOUND));

        placeRepository.delete(place);
    }


}
