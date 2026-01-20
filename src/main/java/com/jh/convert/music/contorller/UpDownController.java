package com.jh.convert.music.contorller;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jh.convert.member.model.dao.MemberRepository;
import com.jh.convert.member.model.vo.Users;
import com.jh.convert.music.model.service.MusicService;
import com.jh.convert.music.model.service.MusicServiceImpl;
import com.jh.convert.music.model.vo.Music;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UpDownController {
	
	private final MemberRepository memberRepository;
	private final MusicServiceImpl musicService;
	
	
	@PostMapping("/music/fileDownload")
	public void fileDownload(@RequestBody Map<String, String> params, 
            				HttpServletResponse res, 
            				@RequestHeader(value="User-Agent") String header) {
		
		// 1. JSON 바디에서 파라미터 추출
		String artist = params.get("artist");
        String title = params.get("title");
        String filePath = params.get("filePath");
        
        
//        String testFolder = "C:/musicTest/";
//        String testFileName = "EVE - 빛.mp3"; // 실제 폴더에 있는 파일명과 정확히 같아야 함
//        
//        File file = new File(testFolder + testFileName);
//        System.out.println("=== 로컬 테스트 시작 ===");
//        System.out.println("찾는 파일 전체 경로: " + file.getAbsolutePath());
//        System.out.println("파일 존재 여부: " + file.exists());
        
        System.out.println("===========================================");
        System.out.println(">>> 다운로드 요청: " + artist + " - " + title);
        System.out.println("가수 : "+artist );
        System.out.println("제목 : "+title );
        System.out.println("경로 : "+filePath );
        System.out.println("===========================================");
        
        
        // 파일 실제 존재 여부 확인
        // DB에 경로가 있어도 실제 폴더에 파일이 없을 수 있으니.
        File file = new File(filePath);
        if(!file.exists() || !file.isFile()) {
        	System.out.println("오류 : 파일이 존재하지 않습니다!!");
        	res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        	return;
        }
        	
        // 2. 파일명 생성
        String fileName = artist + " - " + title + ".mp3";
        
        try {
        	// 한글 깨짐 방지 인코딩
        	// 공백 '+'로 변하는 것 방지
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        	
            // 브라우저에게 파일데이터라고 전달
            res.setContentType("application/octet-stream");
            // 브라우저가 다운로드 창을 띄우고
            // encodedFileName로 파일이름 전달
            res.setHeader("Content-Disposition", "attachment; filename=\""+ encodedFileName + "\"");
            // 파일 크기 전달
            res.setContentLength((int) file.length());
            
            // 스트림
            // InputStream: 서버 파일 읽기용 / OutputStream: 클라이언트에게 전송용
            try(InputStream is = new FileInputStream(file);
            	OutputStream os = res.getOutputStream()){
            	
            	// 8KB크기의 버퍼 생성 -> 한바이트씩이 아닌 뭉텅이로 읽는
            	byte[] buffer = new byte[8192];
            	int bytesRead;
            	
            	// 파일을 끝까지 반복해서 읽고쓰기
            	while((bytesRead = is.read(buffer)) != -1) {
            		os.write(buffer, 0, bytesRead);
            	}
            	// 남은 데이터 모두 출력 후 버퍼 비우기
            	os.flush();
            	System.out.println(">>> 스트림 전송 완료");
            	
            }catch(Exception e) {
            	System.out.println("!!! 다운로드 처리 중 예외 발생");
            	e.printStackTrace();
            }
        	
        }catch(Exception e) {
        	System.out.println("!!! 다운로드 중 서버 오류 발생");
        	e.printStackTrace();
        }
        
	}
	
	@PostMapping("/music/upload")
	public ResponseEntity<?> fileUpload(
			// 파라미터 추출 (JS의 FormData에 담긴 키값과 일치해야 한다)
			@RequestParam("artist") String artist,
			@RequestParam("title") String title,
			// 실제 파일 데이터
			@RequestParam("file") MultipartFile mFile,
			// 로그인한 사용자 정보
			@AuthenticationPrincipal Users user){
		
		// 사용자 검증
		if (user == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 정보가 없습니다.");
	    }
		
		// DB에서 현재 로그인한 유저의 최신 정보를 가져온다.
		String kakaoId = user.getKakaoId();
		Users member = memberRepository.findByKakaoId(kakaoId)
	            .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
		
		// 업로드 테스트 
		// 저장 경로 설정
		// 추후에는 설정파일(properties)로 관리하자.
		String path = "C:/musicTest/";
		File uploadPath = new File(path);
		
		
		String oriFileName = "";
		String ext = "";
		
		try {
			// 파일명 정리(파일명 정규화)
			// 원본 파일 명
			oriFileName = mFile.getOriginalFilename();
			// 확장자 추출
			ext = oriFileName.substring(oriFileName.lastIndexOf("."));
			// 새로운 파일명
			String rename = title + "-" + artist + "_" + System.currentTimeMillis() + ext;
			// String rename = title + "-" + artist + ext;
			
//			File f = new File(path + rename + ext);
//			int count = 1;
//
//			// 파일이 이미 존재한다면 숫자를 계속 높여가며 확인
//			while (f.exists()) {
//			    rename = title + "-" + artist + "(" + count + ")";
//			    f = new File(path + rename + ext);
//			    count++;
//			}
//			
			
			
			// 로컬에 저장(물리적 파일)
			mFile.transferTo(new File(path + rename));
			// mFile.transferTo(f);
			
			
			// DB 저장을 위한 엔티티 (Builder 패턴 사용)
			Music music = Music.builder()
							.title(title)
							.artist(artist)
							.filePath(path+rename)
							.name(member).build();
			
			// DB 저장
			musicService.insertFile(music);
			
			// 성공 응답 반환
			return ResponseEntity.ok().body(Map.of("message", "업로드 성공!"));
		}catch (Exception e) {
			e.printStackTrace();
			// 실패 시 500에러와 에러 메시지 반환
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "업로드 중 오류 발생"));
		}
		
	}
	
	@PostMapping("/music/update/{musicId}")
	public ResponseEntity<?> update(
			@PathVariable("musicId") int musicId,
			@RequestBody Map<String, Object> request,
            @AuthenticationPrincipal Users user){
		
		
	    String title = (String) request.get("title");
	    String artist = (String) request.get("artist");
	    String lyrics = (String) request.get("lyrics");
	    String genre = (String) request.get("genre");
	    String album = (String) request.get("album");
	    String year = (String) request.get("year");
	    
	    Music music = musicService.findById(musicId);
	    System.out.println("=========================");
	    System.out.println("수정 요청 ID: " + musicId);
	    System.out.println("수정할 가수: " + artist);
	    System.out.println("수정할 제목: " + title);
		System.out.println("수정할 가사: " +lyrics);
		System.out.println("수정할 장르: " +genre);
		System.out.println("수정할 앨범: " +album);
		System.out.println("수정할 년도: " +year);
		System.out.println("=========================");
		try {
			// 서비스 단에서 DB 업데이트 로직 수행
			// musicService.updateMusicInfo(musicId, title, artist, lyrics, user);
			Music savedMusic = musicService.updateMusicInfo(musicId, request, user);
			System.out.println("DB 저장 완료! 제목: " + savedMusic.getTitle());
			return ResponseEntity.ok(Map.of(
				"message", "정보가 성공적으로 수정되었습니다.",
				"updatedTitle", savedMusic.getTitle()
			));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(
					HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "수정 중 오류가 발생했습니다."));
	    }
	}
}
