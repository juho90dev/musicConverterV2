package com.jh.convert.login.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import com.jh.convert.redis.model.service.RedisService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {
	
//    @Value("${jwt.secret}")
//    private String secretKey;
	// RedisService 주입
	private final RedisService redisService; 
	
	// [설정값 주입] yml의 access-expiration 값을 가져옴 
    @Value("${jwt.access-expiration}")
    private long accessTokenTime;
    
    // [설정값 주입] yml의 refresh-expiration 값을 가져옴 
    @Value("${jwt.refresh-expiration}")
    private long refreshTokenTime;
    
    // private final long accessTokenValidityTime = 1000L * 60 * 30; // 30분
    // private final long refreshTokenTime = 14 * 24 * 60 * 60 * 1000L; // 14일
    
    
    // 키는 최소 32자 이상의 보안 문자열 [서명에 사용할 키 문자열]
    @Value("${jwt.secret}")
    private String SECRET_KEY;
    // Key 대신 SecretKey 타입 사용[보안키 객체]
    private SecretKey key; 
    

 
    @PostConstruct
    protected void init() {
        // [초기화] 객체 생성 후 SECRET_KEY를 바탕으로 HMAC-SHA 알고리즘용 키를 생성합니다.
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    
    // Access Token 생성
    // public String createAccessToken(String email, String nickname) {
    public String createAccessToken(String kakaoId, String nickname) {
    	
    	Date now = new Date();
    	// System.out.println("넘어온 카카오 아이디 : "+kakaoId);
        String token = Jwts.builder()
                //.subject(email)
                .subject(kakaoId)              // 토큰의 주인(Principal) 설정
                .claim("nickname", nickname)   // 커스텀 클레임: 닉네임 추가
                .claim("role", "ROLE_FAMILY")  // 커스텀 클레임: 권한 추가
                .issuedAt(now)                 // 발행 시간
                .expiration(new Date(now.getTime() + accessTokenTime)) // 만료 시간 (yml 설정값 사용)
                .signWith(key, Jwts.SIG.HS256) // [중요] Jwts.SIG 사용 - HS256 알고리즘으로 서명
                .compact();
     // [로그 추가]
        System.out.println("========== [JWT Access Token 발급] ==========");
        System.out.println("대상 카카오 ID: " + kakaoId);
        System.out.println("만료 시간: " + accessTokenTime);
        System.out.println("발급된 토큰: " + token);
        System.out.println("============================================");
        return token;
    }

    // Refresh Token 생성
    // Access Token 만료 시 재발급을 위해 사용하며, 보안상 최소한의 정보만 담고 유효기간이 길다.
    public String createRefreshToken(String kakaoId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(kakaoId)              // 재발급 시 식별할 사용자 ID만 포함
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenTime)) // 만료 시간 (yml 설정값 사용)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // [정보 추출] 토큰에서 이메일 추출
    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // [정보 추출] 토큰에서 닉네임 추출
    public String getNickname(String token) {
        return parseClaims(token).get("nickname", String.class);
    }
    
    // [정보 추출] 토큰에서 카카오 ID 추출
    public String getId(String token) {
    	return parseClaims(token).getSubject();
    }
    // [정보 추출] 토큰에서 권한 추출
    public String getRole(String token) {
    	return parseClaims(token).get("role", String.class);
    }

    
    
    // 공통 클레임 파싱 로직 (최신 문법)
    // 토큰을 열어서 그 안에 담긴 정보(Claims)를 꺼낸다.
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)          // 우리가 만든 키로 위조 여부 확인
                .build()
                .parseSignedClaims(token) //명된 토큰 해석
                .getPayload(); 			  // 내용물(Claims) 반환
    }

    // 토큰 유효성 검사
    // 토큰이 위조되지 않았는지, 만료되지 않았는지 확인하여 true/false를 반환
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        }catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("토큰이 만료되었습니다: {}", e.getMessage());
            return false;
        }
        catch (JwtException | IllegalArgumentException e) {
            // 모든 JWT 관련 예외(만료, 위조 등)를 여기서 처리 후 false 반환
            return false;
        }
    }
    
 // JwtTokenProvider 내부 추가
    public String refreshAccessToken(String refreshToken, String nickname) {
        // 1. Refresh Token 자체의 유효성(만료 여부, 변조 여부) 검사
    	try {
    		if (validateToken(refreshToken)) {
    			// 2. Refresh Token에서 사용자 식별값(kakaoId) 추출
    			String kakaoId = getId(refreshToken);
    			
    			// 3. Redis에서 해당 카카오 ID의 토큰 가져오기
    			String savedToken = redisService.getRefreshToken(kakaoId);
    			
    			// 4. 전달받은 토큰과 Redis 저장 토큰 비교
                if (savedToken != null && savedToken.equals(refreshToken)) {
                    return createAccessToken(kakaoId, nickname);
                } else {
                    log.warn("Redis에 저장된 토큰과 일치하지 않습니다.");
                }
    			
//    			
//    			
//    			// 3. 추출한 정보로 새로운 Access Token 생성하여 반환
//    			return createAccessToken(kakaoId, nickname);
    		}
    		
    	}catch (Exception e) {
    		log.error("재발급 중 오류 발생: {}", e.getMessage());
		}
        // 4. Refresh Token도 만료되었다면 null이나 예외를 던짐
        return null; 
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
