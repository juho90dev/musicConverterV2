package com.jh.convert.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.jh.convert.login.auth.handler.OAuth2FailureHandler;
import com.jh.convert.login.auth.handler.OAuth2SuccessHandler;
import com.jh.convert.login.auth.jwt.JwtAuthenticationFilter;
import com.jh.convert.login.auth.jwt.JwtTokenProvider;
import com.jh.convert.login.auth.service.CustomOAuth2UserService;
import com.jh.convert.redis.model.service.RedisService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class SecurityConfig {
	
	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2SuccessHandler oAuth2SuccessHandler;
	private final OAuth2FailureHandler oAuth2FailureHandler;
	private final JwtTokenProvider jwtTokenProvider;
	private final RedisService redisService;
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
		http
        .csrf(csrf -> csrf.disable())
        // 1. CORS 설정 추가
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
        		
        	// 누구나 접속 가능(정적 자원)
        	.requestMatchers("/","/css/**", "/js/**", "/images/**", "/asset/**", "/common/**").permitAll()
        	.requestMatchers("/api/auth/refresh","/api/music/stream/**","/api/search/**").permitAll()
        	// html 페이지 주소
        	.requestMatchers("/mypage","/list/**", "/member/**","/music/**").permitAll()
        	
        	// 중요한 데이터 api(가족 인증된 사람만)
            .requestMatchers(
            	"/api/member/**",
            	"/api/list/**",
            	"/api/music/**",
            	"/api/fileDownload",
            	"/api/upDownload",
            	"/api/convert",
            	"/api/search/**"
            ).authenticated()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/")
            .successHandler(oAuth2SuccessHandler)
            .failureHandler(oAuth2FailureHandler)
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)
            )
        )
        .logout(logout -> logout
        	    .logoutUrl("/logout") // 반드시 이 경로로 POST 요청이 와야 함
        	    .addLogoutHandler((request, response, authentication) -> {
        	        String authHeader = request.getHeader("Authorization");
        	        
        	        if (authHeader != null && authHeader.startsWith("Bearer ")) {
        	            try {
        	                String token = authHeader.substring(7);
        	                String kakaoId = jwtTokenProvider.getId(token); // ID 추출
        	                
        	                if (kakaoId != null) {
        	                    redisService.deleteRefreshToken(kakaoId); // Redis 삭제
        	                    System.out.println(">>> 로그아웃 성공 (ID: " + kakaoId + ")");
        	                }
        	            } catch (Exception e) {
        	                // 토큰이 유효하지 않은 경우 무시하거나 로그만 남김
        	            }
        	        }
        	    })
        	    .logoutSuccessHandler((request, response, authentication) -> {
        	        response.setStatus(HttpServletResponse.SC_OK);
        	    })
        	)
        .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), 
                        UsernamePasswordAuthenticationFilter.class);
    
    return http.build();
	}
	
	
	// CORS 설정을 위한 Bean 추가
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:9090")); // 프론트 주소 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 중요: JS에서 Authorization 헤더를 읽을 수 있게 노출함
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // 이 경로는 스프링 시큐리티의 모든 보안 필터 검사에서 완전히 제외됩니다.
        return (web) -> web.ignoring().requestMatchers("/api/music/stream/**");
    }

}
