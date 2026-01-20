package com.jh.convert.music.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jh.convert.music.model.dao.ElasticRepository;
import com.jh.convert.music.model.dao.MusicRepository;
import com.jh.convert.music.model.service.MusicService;
import com.jh.convert.music.model.vo.ElasticDocument;
import com.jh.convert.music.model.vo.Music;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Component
@Slf4j
public class ElasticSyncScheduler {
	private final MusicRepository musicRepository;
	private final MusicService musicService;
	private final ElasticRepository elasticRepository;
	
	// 초 분 시 일 월 요일
	// 매일 새벽 4시에 실행(0초 0분 4시 매일 매월 매요일)
	@Scheduled(cron = "0 0 4 * * *")
	public void dailySync() {
		log.info("Elasticsearch 정기 동기화 시작...");
		try {
            // 전체 데이터를 가져와서 ES에 덮어쓰기 (기존 데이터가 있어도 ID가 같으면 업데이트됨)
            List<Music> allMusic = musicRepository.findAll();
            
            
//            List<ElasticDocument> documents = new ArrayList<>();
//            for (Music music : allMusic) {
//                ElasticDocument doc = ElasticDocument.from(music);
//                documents.add(doc);
//            }
               
            List<ElasticDocument> documents = allMusic.stream()
                    .map(ElasticDocument::from)
                    .toList();
            
            elasticRepository.saveAll(documents);
            log.info("정기 동기화 완료: {}건 처리됨", documents.size());
        } catch (Exception e) {
            log.error("정기 동기화 중 오류 발생: {}", e.getMessage());
        }
		
	}

}
