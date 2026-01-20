package com.jh.convert.login.auth.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler{
	
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException{
		
		// 1. 에러 메시지 한글 깨짐 방지 인코딩
        String errorMessage = UriUtils.encode("등록된 사용자만 사용할 수 있는 서비스입니다.", StandardCharsets.UTF_8);
		
     // 2. 홈 화면으로 리다이렉트 하면서 에러 메시지를 쿼리 파라미터로 전달
        String targetUrl = UriComponentsBuilder.fromUriString("/")
                .queryParam("error", "forbidden")
                .queryParam("message", errorMessage)
                .build().toUriString();

        response.sendRedirect(targetUrl);
	}

}
