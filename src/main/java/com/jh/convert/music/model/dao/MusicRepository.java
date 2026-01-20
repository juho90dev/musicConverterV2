package com.jh.convert.music.model.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jh.convert.member.model.vo.Users;
import com.jh.convert.music.model.vo.Music;

@Repository
public interface MusicRepository extends JpaRepository<Music, Integer>{
	
	// 사용자의 카카오 ID로 조회하며, 페이징 정보를 받아 Slice 형태로 반환
    // 쿼리 메서드 이름 규칙에 따라 자동으로 OrderByMusicIdDesc(최신순) 적용 가능
    // Page<Music> findByName_KakaoIdOrderByMusicIdDesc(String kakaoId, Pageable pageable);
	 @Query("SELECT m FROM Music m JOIN m.name u WHERE u.kakaoId = :kakaoId ORDER BY m.musicId DESC")
     Slice<Music> findByName_KakaoIdOrderByMusicIdDesc(String kakaoId, Pageable pageable);
	
	 
	 // 전체 리스트 조회
	 @Query(value = 
			    "SELECT * FROM ( " +
			    "    SELECT b.*, ROWNUM as rnum FROM ( " +
			    "        SELECT * FROM music " +
			    "        WHERE name = :userId " + 
			    "        ORDER BY music_id DESC " +
			    "    ) b " +
			    "    WHERE ROWNUM <= :maxRow " +
			    ") WHERE rnum > :minRow", 
			    nativeQuery = true)
	List<Music> findMusicByName(
			@Param("userId") int userId, 
	        @Param("maxRow") long maxRow, 
	        @Param("minRow") long minRow);
	List<Music> findByName(Users name);
	
	
//	// 검색어 추가
//	@Query(value = 
//		    "SELECT * FROM ( " +
//		    "    SELECT b.*, ROWNUM as rnum FROM ( " +
//		    "        SELECT * FROM music " +
//		    "        WHERE name = :userId " + 
//		    //"        AND (title LIKE %:keyword% OR artist LIKE %:keyword%) " + // 제목이나 가수로 검색
//		    "		AND (UPPER(title) LIKE '%' || UPPER(:keyword) || '%' " + // 대문자로 변환하여 비교
//		    "			OR UPPER(artist) LIKE '%' || UPPER(:keyword) || '%') " + // 대문자로 변환하여 비교
//		    "       ORDER BY" +
//		    "			CASE WHEN :sort = 'title' THEN title END ASC,"+
//		    "       	CASE WHEN :sort = 'artist' THEN artist END ASC, "+
//		    "			CASE WHEN :sort = 'music_id' THEN music_id END DESC"+
//		    "    ) b " +
//		    "    WHERE ROWNUM <= :maxRow " +
//		    ") WHERE rnum > :minRow", 
//		    nativeQuery = true)
//		List<Music> searchMusicByName(
//		    @Param("userId") int userId, 
//		    @Param("keyword") String keyword, // 검색어 추가
//		    @Param("sort") String sort, // 정렬 필드 추가
//		    @Param("searchType")String searchType,
//		    @Param("maxRow") long maxRow, 
//		    @Param("minRow") long minRow
//		);
	
	
	
	@Query(value = 
		    "SELECT * FROM ( " +
		    "    SELECT b.*, ROWNUM as rnum FROM ( " +
		    "        SELECT * FROM music " +
		    "        WHERE name = :userId " + 
		    "        AND ( " +
		    "            /* 1. 전체 검색일 때 */ " +
		    "            (:searchType = 'all' AND (UPPER(title) LIKE '%' || UPPER(:keyword) || '%' OR UPPER(artist) LIKE '%' || UPPER(:keyword) || '%')) OR " +
		    "            /* 2. 제목 검색일 때 */ " +
		    "            (:searchType = 'title' AND UPPER(title) LIKE '%' || UPPER(:keyword) || '%') OR " +
		    "            /* 3. 가수 검색일 때 */ " +
		    "            (:searchType = 'artist' AND UPPER(artist) LIKE '%' || UPPER(:keyword) || '%') " +
		    "        ) " +
		    "        ORDER BY " +
		    "            CASE WHEN :sort = 'title' THEN title END ASC, " +
		    "            CASE WHEN :sort = 'artist' THEN artist END ASC, " +
		    "            CASE WHEN :sort = 'music_id' THEN music_id END DESC " +
		    "    ) b " +
		    "    WHERE ROWNUM <= :maxRow " +
		    ") WHERE rnum > :minRow", 
		    nativeQuery = true)
		List<Music> searchMusicByName(
		    @Param("userId") int userId, 
		    @Param("keyword") String keyword, 
		    @Param("sort") String sort, 
		    @Param("searchType") String searchType, // 파라미터 전달 확인
		    @Param("maxRow") long maxRow, 
		    @Param("minRow") long minRow
		);

	
	@Query(value = 
		    "SELECT * FROM ( " +
		    "    SELECT b.*, ROWNUM as rn FROM ( " +
		    "        SELECT m.* FROM music m " +
		    "        JOIN users u ON m.name = u.user_id " + // 조인 추가
		    "        WHERE 1=1 " + 
		    "        AND (:ownerName = 'all' OR u.name = :ownerName) " + // 오타 수정 및 'all' 로직 추가
		    "        AND ( " +
		    "            (:searchType = 'all' AND (UPPER(m.title) LIKE '%' || UPPER(:keyword) || '%' OR UPPER(m.artist) LIKE '%' || UPPER(:keyword) || '%')) OR " +
		    "            (:searchType = 'title' AND UPPER(m.title) LIKE '%' || UPPER(:keyword) || '%') OR " +
		    "            (:searchType = 'artist' AND UPPER(m.artist) LIKE '%' || UPPER(:keyword) || '%') " +
		    "        ) " +
		    "        ORDER BY " +
		    "            CASE WHEN :sort = 'title' THEN m.title END ASC, " +
		    "            CASE WHEN :sort = 'artist' THEN m.artist END ASC, " +
		    "            CASE WHEN :sort = 'music_id' THEN m.music_id END DESC " +
		    "    ) b " +
		    "    WHERE ROWNUM <= :maxRow " +
		    ") WHERE rn > :minRow", // rn 별칭 일치
		    nativeQuery = true)
		List<Music> searchAllFamilyMusic(
		        @Param("ownerName") String ownerName, 
		        @Param("keyword") String keyword, 
		        @Param("sort") String sort, 
		        @Param("searchType") String searchType, 
		        @Param("maxRow") long maxRow, 
		        @Param("minRow") long minRow    
		);
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
