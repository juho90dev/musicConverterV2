package com.jh.convert.music.contorller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jh.convert.music.client.ElasticSyncScheduler;
import com.jh.convert.music.model.service.MusicService;
import com.jh.convert.music.model.vo.ElasticDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class ElasticController {
	
	private final MusicService musicService;
	private final ElasticSyncScheduler elasticSyncScheduler;
	
	@Autowired
	private ElasticsearchOperations operations;
	
	@GetMapping("/search/autocomplete")
	public ResponseEntity<List<String>> getAutocomplete(
			@RequestParam("keyword") String keyword){
		
		System.out.println("프론트에서 넘어온 검색어: " + keyword);
		
		List<String> results = musicService.getAutocompleteSuggestions(keyword);
		System.out.println("검색어 [" + keyword + "] 로 찾은 결과 개수: " + results.size());
		
		return ResponseEntity.ok(results);
		//return ResponseEntity.ok(new ArrayList<>());
	}

	
//	@GetMapping("/search/check")
//	public String checkConnection() {
//	    try {
//	        boolean exists = operations.indexOps(IndexCoordinates.of("music")).exists();
//	        return exists ? "엘라스틱서치 연결 성공!" : "연결은 됐는데 music 인덱스가 없어요.";
//	    } catch (Exception e) {
//	        return "연결 실패: " + e.getMessage();
//	    }
//	}
//	
	
	@GetMapping("/search/check")
	public String checkConnection() {
	    try {
	        // 1. 인덱스 존재 여부 확인
	        boolean exists = operations.indexOps(IndexCoordinates.of("music")).exists();
	        
	        if (exists) {
	            // 2. 인덱스가 있다면 동기화 실행
	            log.info("인덱스 확인됨. 수동 동기화를 시작합니다...");
	            elasticSyncScheduler.dailySync(); 
	            return "엘라스틱서치 연결 성공 및 music 인덱스 동기화 완료!";
	        } else {
	            return "연결은 됐는데 music 인덱스가 없어요. 인덱스를 먼저 생성해주세요.";
	        }
	    } catch (Exception e) {
	        log.error("테스트 중 오류 발생", e);
	        return "연결 실패: " + e.getMessage();
	    }
	}
	
}
