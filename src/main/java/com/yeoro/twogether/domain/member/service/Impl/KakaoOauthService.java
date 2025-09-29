package com.yeoro.twogether.domain.member.service.Impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeoro.twogether.domain.member.dto.KakaoProfile;
import com.yeoro.twogether.domain.member.dto.OauthProfile;
import com.yeoro.twogether.domain.member.entity.LoginPlatform;
import com.yeoro.twogether.domain.member.service.OauthService;
import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service("KAKAO")
@RequiredArgsConstructor
public class KakaoOauthService implements OauthService {

    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${kakao.client-id}")      private String clientId;
    @Value("${kakao.client-secret:}") private String clientSecret;

    @Override
    public LoginPlatform platform() { return LoginPlatform.KAKAO; }

    /** 인가 URL */
    @Override
    public String buildAuthorizeUrl(String redirectUri, String state) {
        return UriComponentsBuilder.fromHttpUrl("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build(true).toUriString();
    }

    /** code→access_token */
    @Override
    public String exchangeCodeForAccessToken(String code, String redirectUri) {
        String url = "https://kauth.kakao.com/oauth/token";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) form.add("client_secret", clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> res = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
            throw new ServiceException(ErrorCode.KAKAO_API_ERROR);
        }
        try {
            record KakaoToken(@JsonProperty("access_token") String accessToken) {}
            return objectMapper.readValue(res.getBody(), KakaoToken.class).accessToken();
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.KAKAO_PROFILE_PARSE_FAILED, e);
        }
    }

    /** access_token→프로필 */
    @Override
    public OauthProfile getUserProfile(String accessToken) {
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

        try {
            KakaoProfile kakaoProfile = objectMapper.readValue(response.getBody(), KakaoProfile.class);

            OauthProfile profile = new OauthProfile();
            var account = kakaoProfile.getKakaoAccount();
            var info    = account.getProfile();

            profile.setEmail(account.getEmail());
            profile.setPhoneNumber(account.getPhoneNumber());
            profile.setName(info.getNickname());
            profile.setProfileImageUrl(info.getProfileImageUrl());
            profile.setPlatformId(String.valueOf(kakaoProfile.getId()));
            profile.setGender(account.getGender());
            profile.setAgeRange(account.getAgeRange());
            return profile;
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.KAKAO_PROFILE_PARSE_FAILED, e);
        }
    }

    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}