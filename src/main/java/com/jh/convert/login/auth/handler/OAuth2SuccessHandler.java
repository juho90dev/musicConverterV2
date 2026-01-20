package com.jh.convert.login.auth.handler;

import com.jh.convert.login.auth.jwt.JwtTokenProvider;
import com.jh.convert.redis.model.service.RedisService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    
    @Value("${jwt.refresh-expiration}")
    private long refreshTokenTime; // 만료 시간 설정을 위해 가져옴
    
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // 1. 로그인 성공한 사용자 정보 추출 (Principal은 소셜 로그인 정보를 담고 있음)
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        // 2. 카카오 계정 및 프로필 정보 파싱 [JSON 구조]  (kakao_account -> profile)
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        
        // 1. 사용자 정보들 추출
        // Long kakaoId = (Long) attributes.get("id");
        // 고유 식별 ID (Long타입을 String으로 변환)
        String kakaoIdStr = String.valueOf(attributes.get("id"));
        String email = (String) kakaoAccount.get("email");
        String nickname = (String) profile.get("nickname");

        
        // 3. 서비스 전용 JWT 토큰 생성
        // String accessToken = jwtTokenProvider.createAccessToken(email, nickname);
        String accessToken = jwtTokenProvider.createAccessToken(kakaoIdStr, nickname);
        String refreshToken = jwtTokenProvider.createRefreshToken(kakaoIdStr);
        log.info("==============================================="); 
        log.info("OAuth2 로그인 성공: 카카오ID={}, 닉네임={}", kakaoIdStr, nickname);
        log.info("==============================================="); 
        
        
        // 2. Redis에 저장 (카카오ID를 키로 사용)
        redisService.saveRefreshToken(kakaoIdStr, refreshToken, refreshTokenTime);
        log.info("Redis에 Refresh Token 저장 완료: {}", kakaoIdStr);
        
        
        // 4. 프론트엔드로 리다이렉트 (쿼리 파라미터에 토큰을 실어서 전달)
        // JS에서 이 URL의 파라미터를 읽어 로컬 스토리지에 저장하게 됩니다.
        String targetUrl = UriComponentsBuilder.fromUriString("/")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("nickname", UriUtils.encode(nickname, StandardCharsets.UTF_8))
                .build().toUriString();

        // 5. 실제 리다이렉트 수행
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}