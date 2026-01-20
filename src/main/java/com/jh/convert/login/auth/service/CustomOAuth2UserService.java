package com.jh.convert.login.auth.service;

import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jh.convert.member.model.dao.MemberRepository;
import com.jh.convert.member.model.vo.Users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService{
	
	private final MemberRepository memberRepository;
	// 1. 허용된 가족들의 카카오 ID 리스트 (예시 ID)
    private final List<String> ALLOWED_FAMILY_IDS = List.of(
        "123456789", // 아빠 카카오 ID
        "987654321", // 엄마 카카오 ID
        "987698761", // 엄마 카카오 ID
        "123412341"  // 나 (본인)
    );
	
	
	
	@Override
    @Transactional // DB 처리를 위해 트랜잭션 추가
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 유저 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        
        // 카카오 고유 ID 추출 및 검증
        String kakaoId = String.valueOf(attributes.get("id"));
        // Long kakaoId = Long.parseLong(kakaoIdStr);
        log.info("로그인 시도 유저 ID: {}", kakaoId);
        
     // 2. [핵심] DB에서 해당 kakaoId를 가진 유저가 있는지 확인
        // (가족만 이용하므로, DB에 없는 유저라면 여기서 바로 예외 발생)
        Users member = memberRepository.findByKakaoId(String.valueOf(kakaoId))
                .orElseThrow(() -> new OAuth2AuthenticationException("허가되지 않은 가족 구성원입니다."));
     
//      DB에서 가져온 데이터 확인
//     log.info("[인증 성공] DB에서 가족 정보를 찾았습니다!");
//     log.info("고유번호(PK): {}", member.getUserId());
//     log.info("이름(가족관계): {}", member.getName());
//     log.info("DB에 저장된 닉네임: {}", member.getNickname());
//     log.info("권한: {}", member.getRole());
        // 카카오 유저 정보 파싱
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String nickname = (String) profile.get("nickname");

        // 3. DB 저장 및 업데이트 로직 추가
        // 이메일로 기존 회원을 찾아 정보를 갱신하거나, 없으면 새로 생성합니다.
        // 카카오고유 아이디로 기존 회원을 찾아 정보를 갱신하거나, 없으면 새로 생성합니다.
//        memberRepository.findByEmail(kakaoId)
//            .map(member -> {
//                // 이미 가입된 유저라면 닉네임 등을 최신화
//                member.setNickname(nickname);
//                return memberRepository.save(member);
//            })
//            .orElseGet(() -> {
//                // 신규 유저라면 가입 처리 (Insert)
//                Member newMember = Member.builder()
//                        .email(email)
//                        .nickname(nickname)
//                        .role("ROLE_USER") // 기본 권한 설정
//                        .build();
//                return memberRepository.save(newMember);
//            });

        return oAuth2User;
    }

}
