package com.jh.convert.login.auth.jwt;

import java.io.IOException;
import java.util.Collections;

import org.springframework.web.filter.OncePerRequestFilter;

import com.jh.convert.member.model.vo.Users;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;


@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter{
	
	private final JwtTokenProvider jwtTokenProvider;

	
	@Override
    protected void doFilterInternal(HttpServletRequest request, 
    		HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

		// System.out.println("현재 필터 통과 중인 URI: " + request.getRequestURI());
		// 1. 헤더에서 토큰 꺼내기
        String token = resolveToken(request);
        String requestURI = request.getRequestURI();
        String path = request.getRequestURI();
        
        // 재발급 경로는 필터 로직을 수행하지 않고 즉시 통과
        if (path.equals("/api/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        
        // log.info("필터 통과 중 - 경로: {}, 토큰 존재 여부: {}", requestURI, (token != null));
        // 2. 토큰 유효성 검사
        if (token != null && jwtTokenProvider.validateToken(token)) {
        	
        	
        	// 3. 토큰에서 정보 추출 (이메일, 닉네임 등)
            String email = jwtTokenProvider.getEmail(token);
            String nickname = jwtTokenProvider.getNickname(token);
            String kakaoId = jwtTokenProvider.getId(token);
            String roles = jwtTokenProvider.getRole(token);
//            java.util.Map<String, String> principalMap = new java.util.HashMap<>();
//            principalMap.put("kakaoId", kakaoId);
//            principalMap.put("nickname", nickname);
            //log.info("인증 성공 - 이메일: {}, 닉네임: {}", email, nickname);
            // System.out.println("카카오아이디 : " + kakaoId);
            // 4. 인증 객체 생성 (Principal에 이메일 혹은 닉네임을 설정)
            // 
            
            Users principal = Users.builder().kakaoId(kakaoId)
            						.nickname(nickname)
            						.email(email)
            						.role(roles)
            						.build();
            
            
            
            // 토큰에서 ROLE_FAMILY 등을 추출하는 메서드 추가 필요
            String role = jwtTokenProvider.getRole(token); 
            Authentication auth = new UsernamePasswordAuthenticationToken(
            		principal, // 주체 (Principal)
                    null,  // 비밀번호 (JWT이므로 필요없음)
                    Collections.singletonList(new SimpleGrantedAuthority(role)) // 권한
            );

            // 5. 시큐리티 컨텍스트에 인증 정보 저장
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        else {
        	if (path.startsWith("/api/")) {
                log.info("API 요청 중 토큰 만료 감지: {}", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Unauthorized\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
	
	// Request Header에서 토큰 정보 추출
	// Header Format: [Authorization: Bearer {token}]
    private String resolveToken(HttpServletRequest request) {
    	
    	java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            //log.info("Header [{}]: {}", name, request.getHeader(name));
        }
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 이후의 토큰값만 반환
        }
        return null;
    }

}
