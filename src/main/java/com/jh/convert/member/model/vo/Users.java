package com.jh.convert.member.model.vo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@Getter
@Entity
@NoArgsConstructor
@SequenceGenerator(name="seq_userId", sequenceName="seq_userId", allocationSize = 1)
public class Users {
	
	
	@Id
	@GeneratedValue(generator = "seq_userId", strategy = GenerationType.SEQUENCE)
	private int userId;
	
	// @Column(unique = true, nullable = false)
	private String kakaoId; // 카카오에서 주는 고유 번호 (예: 123123123)
	

    private String name;     // father, mother...
    private String nickname; // 실제 카카오 닉네임
    private String email;    // 카카오 이메일
    private String role;     // 권한 (예: ROLE_FAMILY)
    
}
