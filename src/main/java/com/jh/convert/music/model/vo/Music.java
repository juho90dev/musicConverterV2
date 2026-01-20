package com.jh.convert.music.model.vo;

import com.jh.convert.member.model.vo.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@SequenceGenerator(name="seq_musicId", sequenceName="seq_musicId", allocationSize = 1)
public class Music {
	
	@Id
	@GeneratedValue(generator = "seq_musicId", strategy = GenerationType.SEQUENCE)
	private int musicId;

	// 가수
	@Column(columnDefinition = "varchar2(500) not null")
	private String artist;
	
	// 제목
	@Column(columnDefinition = "varchar2(1000) not null")
	private String title;
	
	// 가사
	@Column(columnDefinition = "varchar2(4000)")
	private String lyrics;
	
	// 장르
	private String genre;
	
	// 앨범
	private String album;
	
	// 앨범 표지
	private String albumCover;
	
	// 발매일
	private String year;
	
	// 파일 경로
	private String filePath;
	
	// 등록한 사용자
	@ManyToOne
	@JoinColumn(name="name")
	private Users name;

}
