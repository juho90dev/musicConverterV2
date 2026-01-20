package com.jh.convert.redis.model.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {
	
	private final StringRedisTemplate redisTemplate;
	
	
	// Refresh Token 저장 (Key: KakaoId, Value: Token)
    public void saveRefreshToken(String kakaoId, String refreshToken, long expirationTime) {
    	// opsForValue() : Strings 구조를 다룰 때 사용
    	redisTemplate.opsForValue().set(
    		// Key에 접두사를 붙이는 것이 관례입니다 (예: RT:12345)
    		"RT:" + kakaoId,
    		refreshToken,
    		// 밀리초 단위를 Duration으로 깔끔하게 전달
    		Duration.ofMillis(expirationTime)
    			
        );
    }
    
    
 // Refresh Token 조회
    public String getRefreshToken(String kakaoId) {
        return redisTemplate.opsForValue().get("RT:" + kakaoId);
        // return null;
    }

    // 로그아웃 시 삭제
    public void deleteRefreshToken(String kakaoId) {
        redisTemplate.delete("RT:" + kakaoId);
    }
	

}
