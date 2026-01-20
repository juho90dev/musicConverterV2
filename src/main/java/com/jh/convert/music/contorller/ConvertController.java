package com.jh.convert.music.contorller;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;


import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jh.convert.member.model.vo.Users;
import com.jh.convert.music.model.service.MusicService;
import com.jh.convert.music.model.vo.Music;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
@Slf4j
public class ConvertController {
	
	private final MusicService musicService;
	

    
	@PostMapping("/music/convert")
    public ResponseEntity<?> convert(@RequestBody Map<String, String> request,
    		@AuthenticationPrincipal Users user) {
		
		// 일단은 요청이 잘 오는지 출력
		System.out.println("변환 요청 수신: " + request.get("videoUrl"));
		System.out.println("변환 요청 수신: " + request.get("artist"));
		System.out.println("변환 요청 수신: " + request.get("title"));
		System.out.println("요청 유저(카카오ID): " + user.getKakaoId());
		try {
			musicService.musicConvert(request, user);
		}catch (IllegalArgumentException e){
			// 서비스에서 던진 "URL 오류"나 "입력값 누락" 메시지를 그대로 프론트에 전달 (400 에러)
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}catch (Exception e){
			e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "서버 내부 오류로 변환에 실패했습니다."));
		}
		
        // 성공 응답 반환
        return ResponseEntity.ok(Map.of("message", "컨트롤러 연결 성공!"));
    }
	
	
	
	@GetMapping("/music/stream/{musicId}")
	public void streamMusic(@PathVariable("musicId") int musicId,
			HttpServletResponse res) {
		Music music = musicService.findById(musicId);
		File file = new File(music.getFilePath());
		
		if (!file.exists()) {
	        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
	        return;
	    }

	    // 재생을 위해 Content-Type을 audio/mpeg (또는 audio/wav 등)으로 설정
	    res.setContentType("audio/mpeg");
	    res.setContentLength((int) file.length());

	    try (InputStream is = new FileInputStream(file);
	         OutputStream os = res.getOutputStream()) {
	        
	        byte[] data = new byte[8192];
	        int read;
	        while ((read = is.read(data)) != -1) {
	            os.write(data, 0, read);
	        }
	        os.flush();
	    } catch (Exception e) {
	        System.out.println("스트리밍 중단: " + e.getMessage());
	    }
	}
}
