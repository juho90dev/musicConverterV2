package com.jh.convert.member.model.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jh.convert.member.model.vo.Users;

@Repository
public interface MemberRepository extends JpaRepository<Users, Integer>{
	
	// 1. 카카오 고유 ID(kakaoId)로 가족 구성원 찾기
    // CustomOAuth2UserService에서 String.valueOf(attributes.get("id"))로 검증할 때 사용
    Optional<Users> findByKakaoId(String kakaoId);

    // 2. 이메일로 가족 구성원 찾기 (필요 시)
    Optional<Users> findByEmail(String email);

    // 3. 만약 특정 이름(father, mother)으로 찾고 싶다면
    Optional<Users> findByName(String name);

}
