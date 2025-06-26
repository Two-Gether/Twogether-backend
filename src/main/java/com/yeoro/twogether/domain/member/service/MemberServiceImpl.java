package com.yeoro.twogether.domain.member.service;

import com.yeoro.twogether.domain.member.entity.LoginPlatform;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.repository.MemberRepository;
import com.yeoro.twogether.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.yeoro.twogether.global.exception.ErrorCode.MEMBER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    /**
     * 이메일이 이미 존재하는지 확인
     */
    @Override
    public boolean isExistEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    /**
     * 이메일을 기반으로 회원 ID 조회
     * 존재하지 않으면 ServiceException 발생
     */
    @Override
    public Long getMemberId(String email) {
        return memberRepository.findByEmail(email)
                .map(Member::getId)
                .orElseThrow(() -> new ServiceException(MEMBER_NOT_FOUND));
    }

    /**
     * <p>소셜 로그인으로 회원가입 처리</p>
     * <p>일부 필드(email, platformId)는 null 가능</p>
     * 저장 후 생성된 회원 ID 반환
     */
    @Override
    @Transactional
    public Long signupByOauth(String email, String nickname, String profileImage, LoginPlatform loginPlatform, String platformId) {
        Member newMember = Member.builder()
                .email(email)  // null일 수 있음
                .nickname(nickname)
                .profileImageUrl(profileImage)
                .loginPlatform(loginPlatform)
                .platformId(platformId)   // null일 수 있음
                .build();

        return memberRepository.save(newMember).getId();
    }

    /**
     * 특정 플랫폼 ID가 이미 등록되어 있는지 확인
     */
    @Override
    public boolean isExistPlatformId(String platformId) {
        return memberRepository.existsByPlatformId(platformId);
    }

    /**
     * <p>플랫폼 ID를 기반으로 회원 ID 조회</p>
     * 없으면 ServiceException 발생
     */
    @Override
    public Long getMemberIdByPlatformId(String platformId) {
        return memberRepository.findByPlatformId(platformId)
                .map(Member::getId)
                .orElseThrow(() -> new ServiceException(MEMBER_NOT_FOUND));
    }

    /**
     * <p>회원 ID를 통해 닉네임 조회</p>
     * 없으면 ServiceException 발생
     */
    @Override
    public String getNicknameByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .map(Member::getNickname)
                .orElseThrow(() -> new ServiceException(MEMBER_NOT_FOUND));
    }

    /**
     * <p>회원 ID를 기반으로 해당 회원의 파트너 ID 조회</p>
     * 파트너가 없을 경우 null 반환
     */
    @Override
    public Long getPartnerId(Long memberId) {
        return memberRepository.findById(memberId)
                .map(Member::getPartner)
                .map(Member::getId)
                .orElse(null);  // 파트너가 없을 수 있음
    }
}
