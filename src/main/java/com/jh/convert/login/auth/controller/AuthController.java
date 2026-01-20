package com.jh.convert.login.auth.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jh.convert.login.auth.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
	
	
	private final JwtTokenProvider jwtTokenProvider;
	
	@PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        
        log.info("Access Token 재발급 요청 시작");

        // 1. Refresh Token 유효성 검사 및 새 Access Token 생성
        // Provider에 이미 만들어두신 refreshAccessToken 메서드를 활용.
        // 닉네임 정보가 토큰에 필요하므로, 리프레시 토큰에서 닉네임을 꺼내거나 
        // 우선은 기본값을 전달한 뒤 Provider 내부 로직을 사용
        String nickname = "USER"; // 혹은 jwtTokenProvider.getNickname(refreshToken)
        String newAccessToken = jwtTokenProvider.refreshAccessToken(refreshToken, nickname);

        if (newAccessToken != null) {
            log.info("Access Token 재발급 성공");
            // 프론트엔드 authFetch에서 data.accessToken으로 접근하므로 키 이름을 맞춰줍니다.
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } else {
            log.warn("Refresh Token이 만료되었거나 유효하지 않습니다.");
            // 401 에러를 내려주어 프론트엔드가 handleLogout()을 실행하게 합니다.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("세션이 만료되었습니다. 다시 로그인해주세요.");
        }
    }

}
