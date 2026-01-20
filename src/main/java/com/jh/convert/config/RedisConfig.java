package com.jh.convert.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableRedisRepositories // Redis Repository 활성화
public class RedisConfig {
	
	
	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		// 별도 설정 없으면 redis의 기본 포트인 localhost:6379로 자동 연결
		return new LettuceConnectionFactory();
	}
	
	
	
	@Bean
	public RedisTemplate<String, Object> redisTemplate(){
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory());
		
		// Key와  Value를 인간이 읽을 수 있는 문자열 형태로 저장하기 위한 설정
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new StringRedisSerializer());
		
		return redisTemplate;
	}

}
