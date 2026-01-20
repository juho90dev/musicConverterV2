package com.jh.convert.music.contorller;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Sort;
import com.jh.convert.member.model.dao.MemberRepository;
import com.jh.convert.member.model.vo.Users;
import com.jh.convert.music.model.service.MusicService;
import com.jh.convert.music.model.vo.Music;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
@Slf4j
public class ListController {
	
	private final MusicService musicService;
	private final MemberRepository memberRepository;

	
	// 무한스크롤 
	@GetMapping("/list")
    public ResponseEntity<?> getMyMusicList(
            // 인증된 사용자 정보 
    		// Spring Security가 현재 로그인한 사용자의 정보를 'principal' 객체에 담아준다
            @AuthenticationPrincipal Users principal,
            // 쿼리 파라미터
            // keyword: 검색어, sort: 정렬기준, searchType: 검색 조건
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", defaultValue = "music_id") String sort,
            @RequestParam(value = "searchType", defaultValue = "all") String searchType,
            
            // 페이징 설정
            // pageable: 현재 몇 페이지인지, 한 페이지에 몇 개(size=20)씩 가져올지 결정합니다.
            @PageableDefault(size = 15) Pageable pageable
            ) {
		
		// 로그인 여부 체크
        if (principal == null) {
            System.out.println("⚠️ 인증된 사용자 정보가 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        
        
        // Principal 객체에서 필요한 정보(카카오ID, 닉네임, 권한)를 추출.
        String kakaoId = principal.getKakaoId();
        String nickname = principal.getNickname();
        String role = principal.getRole();
        
        // DB에서 사용자 정보 재확인
        // 보안과 데이터 정확성을 위해
        Users member = memberRepository.findByKakaoId(String.valueOf(kakaoId))
        		.orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));;
        		
        String name = member.getName();
        
        // 서비스 호출
        // Slice<Music>을 사용
        // Page는 전체 개수를 카운트하지만, Slice는 다음 페이지가 있는지만 확인
        Slice<Music> musicSlice = musicService.findByName(principal,keyword,sort,searchType, pageable);
        
        // 결과 반환(200 OK와 함께 잘라낸 음악 목록 데이터)
        return ResponseEntity.ok(musicSlice);

        
    }
	
	// 무한스크롤 필터
	@GetMapping("/list/all")
	public ResponseEntity<Slice<Music>> getAllFamilyList(
			// 파라미터 추출
			// ownerName: 프론트에서 보낸 가족 필터 값
			@RequestParam(name = "ownerName",required = false) String ownerName,
			@RequestParam(name = "keyword",defaultValue = "") String keyword,
			@RequestParam(name = "searchType",defaultValue = "all") String searchType,
			@RequestParam(value = "sort", defaultValue = "music_id") String sort,
			@PageableDefault(size = 20) Pageable pageable) {
		
		// 한글 필터명을 시스템용 영문 식별자로 변환 - DB에 영문으로 저장되어있음.
		// '로그인한 사용자'가 아닌 '전체' 혹은 '선택된 가족'을 조회하는 서비스를 호출해야 함
		String filterName = ownerName;
		if ("아빠".equals(ownerName)) {
			filterName = "father";
		} else if ("엄마".equals(ownerName)) {
			filterName = "mother";
		} else if ("형아".equals(ownerName)) {
			filterName = "dongheon";
		} else if ("동생".equals(ownerName)) {
			filterName = "juho";
		} else if (ownerName == null || "all".equals(ownerName)) {
			// 필터가 없거나 '전체'를 선택한 경우
			filterName = "all";
		}
		
		// 서버 콘솔에서 확인
		System.out.println("가족필터: " + filterName);
		System.out.println("검색어: " + keyword);
		System.out.println(searchType);
		
		// 서비스 호출
		// 일반 List와 달리 필터가 추가
		Slice<Music> result = musicService.findAllFamilyList(filterName, keyword, sort, searchType, pageable);
		
		// 결과 반환
		// Slice 형태의 데이터를 JSON으로 변환하여 반환
		return ResponseEntity.ok(result);
	}
	
	
	
	// 상세 정보
	@GetMapping("/music/musicinfo/{id}")
	public ResponseEntity<?> getMusicDetail(@PathVariable(name = "id") int id) {
		log.info("노래 상세 정보 요청 ID: {}", id);
		
		// 서비스에서 데이터 가져오기
		Music music = musicService.findById(id);
	    
	    // Optional로 리턴될 경우 .orElseThrow 등을 사용해 예외처리.
	    return ResponseEntity.ok(music);
	}
	
	
	
	
	// 삭제 기능
	@DeleteMapping("/music/{musicId}")
	public ResponseEntity<?> deleteMusic(@PathVariable("musicId")int musicId,
								@AuthenticationPrincipal Users user){
		
		System.out.println("삭제 요청 들어옴 ID: " +musicId); // 1. DB에서 해당 곡 조회
		Music music = musicService.findById(musicId);
		
		if(music == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","존재하지 않는 곡입니다."));
		}
		
		// 2. 권한 확인(업로드한 유저와 삭제하려는 유저가 같은지)
		// music.getName().getKakaoId()는 DB에 저장된주인 KakaoID
		// user.getId()는 현재 로그인해서 삭제 버튼을 누른 사람의 ID
		if(!music.getName().getKakaoId().equals(user.getKakaoId())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","본인이 업로드한 곡만 삭제할 수 있습니다."));
		}
		
		try {
			Music deletedMusic = musicService.deleteMusic(music);
			return ResponseEntity.ok(Map.of( "message", "성공적으로 삭제되었습니다.", "deletedTitle",deletedMusic.getTitle() ));
		}catch(Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("message", "삭제 중 서버 오류 발생")); }
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
