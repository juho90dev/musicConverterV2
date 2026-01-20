package com.jh.convert.view.contorller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ViewController {
	
	@GetMapping("/")
	public String index(Model model) {
	    return "index"; 
	}
	
	
	@GetMapping("/logout")
	public String logout() {
	    // 서버측 세션이 혹시 남아있다면 무효화하고 리다이렉트
	    return "redirect:/"; 
	}
	
	@GetMapping("/mypage")
    public String myPage() {
        // return "member/myPage"; 
        return "member/myPage"; 
    }
	@GetMapping("/list/testpage")
	public String testpage() {
		// return "member/myPage"; 
		return "list/testPage"; 
	}
	@GetMapping("/music/upload")
	public String upload() {
		// return "member/myPage"; 
		return "music/upload"; 
	}
	
	// 노래 목록
	@GetMapping("/music/musiclist")
	public String musicList() {
		// return "member/myPage"; 
		return "music/musiclist"; 
	}
	
	// 노래 목록
	@GetMapping("/music/alllist")
	public String allList() {
		// return "member/myPage"; 
		return "music/alllist"; 
	}
	
	// 노래 상세 정보
	@GetMapping("/music/musicinfo")
	public String musicInfo(@RequestParam(name = "id", required = false) Long id) {
		// return "member/myPage"; 
		System.out.println(id);
		return "music/musicInfo"; 
	}
	
	@GetMapping("/api/member/me")
    public String getCurrentUser(@AuthenticationPrincipal String userId) {
        return "현재 로그인한 카카오 ID: " + userId;
    }
	
	
	
//	@GetMapping("/api/member/history\"")
//	@ResponseBody
//    public List<Map<String, Object>> getHistory() {
//        // 실제 로직 대신 빈 리스트 [] 를 리턴합니다.
//        List<Map<String, Object>> emptyList = new ArrayList<>();
//        
//        // 만약 테스트로 하나 보고 싶다면 아래 주석을 해제하세요.
//        /*
//        Map<String, Object> testData = new HashMap<>();
//        testData.put("id", 1);
//        testData.put("status", "COMPLETED");
//        testData.put("title", "테스트용 노래 제목");
//        testData.put("requestDate", "2023-10-27");
//        emptyList.add(testData);
//        */
//
//        return new ArrayList();
//    }
	
	
	
}
