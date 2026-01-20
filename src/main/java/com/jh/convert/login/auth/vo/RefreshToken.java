package com.jh.convert.login.auth.vo;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@RedisHash(value = "refresh_token")
public class RefreshToken {
	
	
	// 카카오 고유 ID를 String으로 변환해서 넣거나, Long 그대로 사용 가능하다.
    // 하지만 Redis Key의 가독성을 위해 String을 주로 사용.
	@Id
	private String kakaoId;
	
	private String refreshToken;
	
	// 만료 시간 (초 단위)
	@TimeToLive
	private Long expiration;

}
