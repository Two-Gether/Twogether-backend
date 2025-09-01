package com.yeoro.twogether.domain.member.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeoro.twogether.domain.member.dto.KakaoProfile;
import com.yeoro.twogether.domain.member.dto.OauthProfile;
import com.yeoro.twogether.domain.member.service.OauthService;
import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class KakaoOauthService implements OauthService {

    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 클라이언트에서 전달받은 Kakao Access Token을 이용해 사용자 정보를 가져옴
     */
    @Override
    public OauthProfile getUserProfile(String accessToken) {
        String kakaoResponse = callKakaoApi(accessToken);
        KakaoProfile kakaoProfile = parseKakaoProfile(kakaoResponse);
        return convertToOauthProfile(kakaoProfile);
    }

    /**
     * 비밀번호 암호화
     */
    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Kakao API 호출
     */
    private String callKakaoApi(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    request,
                    String.class
            );
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.KAKAO_API_ERROR, e);
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new ServiceException(ErrorCode.KAKAO_API_ERROR);
        }

        return response.getBody();
    }

    /**
     * Kakao 응답 JSON을 KakaoProfile 객체로 파싱
     */
    private KakaoProfile parseKakaoProfile(String kakaoResponse) {
        try {
            return objectMapper.readValue(kakaoResponse, KakaoProfile.class);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.KAKAO_PROFILE_PARSE_FAILED, e);
        }
    }

    /**
     * KakaoProfile → 공통 OauthProfile DTO 변환
     */
    private OauthProfile convertToOauthProfile(KakaoProfile kakaoProfile) {
        KakaoProfile.KakaoAccount account = kakaoProfile.getKakaoAccount();
        KakaoProfile.Profile kakaoProfileInfo = account.getProfile();

        OauthProfile profile = new OauthProfile();
        profile.setEmail(account.getEmail());
        profile.setPhoneNumber(account.getPhoneNumber());
        profile.setName(kakaoProfileInfo.getNickname());
        profile.setProfileImageUrl(kakaoProfileInfo.getProfileImageUrl());
        profile.setPlatformId(String.valueOf(kakaoProfile.getId()));
        profile.setBirthday(account.getBirthday());
        profile.setGender(account.getGender());
        profile.setAgeRange(account.getAgeRange());

        return profile;
    }
}
