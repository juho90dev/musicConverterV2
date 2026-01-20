package com.jh.convert.music.model.service;


import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import com.jh.convert.member.model.vo.Users;
import com.jh.convert.music.model.vo.ElasticDocument;
import com.jh.convert.music.model.vo.Music;

public interface MusicService {
	
	// 무한 스크롤용 음악 리스트 조회
	// @param kakaoId 사용자 식별값
	// @param page 현재 페이지 번호
	// @param size 가져올 개수
	// @return Slice 객체 (데이터 및 다음 페이지 여부 포함)
	// Page<Music> getMyMusicList(String kakaoId, int page, int size);
	//Slice<Music> getMyMusicList(String kakaoId, int page, int size);
	
	Slice<Music> findByName(Users user,String keyword,String sort, String searchType, Pageable pageable);
	
	List<Music> findByName(Users user);
	
	Music findById(int id);

	// 음원 업로드
	Music insertFile(Music music);
	
	// 음원 삭제
	Music deleteMusic(Music music);
	
	// [변환] 성공하면 DB에 저장된 Music 엔티티를 반환
	Music musicConvert(Map<String, String> request, Users user) throws Exception;
	
	Slice<Music> findAllFamilyList(String ownerName, String keyword, String sort, String searchType, Pageable pageable);
	
	
	Music updateMusicInfo(int musicId, Map<String, Object> request, Users user);
	
	List<String> getAutocompleteSuggestions(String keyword);
	
	public void syncOracleToElastic();
	
}
