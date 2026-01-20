package com.jh.convert.music.model.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jh.convert.member.model.dao.MemberRepository;
import com.jh.convert.member.model.vo.Users;
import com.jh.convert.music.client.MusicConvertClient;
import com.jh.convert.music.model.dao.ElasticRepository;
import com.jh.convert.music.model.dao.MusicRepository;
import com.jh.convert.music.model.vo.ElasticDocument;
import com.jh.convert.music.model.vo.Music;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MusicServiceImpl implements MusicService {
	
	private final MusicRepository musicRepository;
	private final MemberRepository memberRepository;
	private final MusicConvertClient musicConvertClient;
	private final ElasticRepository elasticRepository;
	
	
	// 음악 저장 폴더 최상위 경로
    private final String BASE_PATH = "C:\\musicTest\\";
	
// =====================================================================================================================	
// 1. 무한 스크롤	 
// =====================================================================================================================	
	// 리스트 (무한 스크롤)
	@Override
    public Slice<Music> findByName(Users principal,String keyword, String sort,String searchType, Pageable pageable) {
		
		// 사용자 재검증
		// 보안을 위해 principal의 정보를 바탕으로 DB에서 최신 유저 정보
        Users member = memberRepository.findByKakaoId(principal.getKakaoId())
        		.orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));;
        		
        int userId = member.getUserId();
        System.out.println("조회하려는 유저 번호: " + userId);
        
        
        // Oracle 11g 페이징 파라미터 계산
        // Oracle 11g는 최신 버전처럼 OFFSET/FETCH 문법이 없다........
        int pageSize = pageable.getPageSize();
        int minRow = (int) pageable.getOffset();     // 시작 (0, 10, 20...)
        int maxRow = minRow + pageSize;              // 끝 (10, 20, 30...)
        
        // 확인
        System.out.println("[DEBUG] userId: " + userId + " | min: " + minRow + " | max: " + maxRow);

        
        // 검색어 가공
        // keyword가 null이면 빈 문자열("")로 처리 (DB의 LIKE '%%'는 전체 검색이 됨)
        String searchKeyword = (keyword == null) ? "" : keyword;
        
        // Repository 호출
        // 계산된 범위(minRow, maxRow)와 검색 조건을 들고 실제 DB 리스트
        List<Music> list = musicRepository.searchMusicByName(
                userId, 
                searchKeyword, 
                sort, 
                searchType,
                maxRow, 
                minRow
            );
        
        // 초기값은 다음 페이지 없음(false)으로 시작
//        boolean hasNext = false;
//        
//        // 요청한 사이즈보다 많이 가져왔을 때
//        if (list.size() > pageSize) {
//            hasNext = true;
//        } 
//        // 혹은 간단하게 가져온 개수가 pageSize와 같으면 다음 페이지가 있을 가능성이 있다고 판단
//        hasNext = list.size() == pageSize;

        boolean hasNext = (list.size() == pageSize);
        // 4. 결과를 다시 Slice 객체로 감싸서 반환 (컨트롤러/JS 코드 수정 불필요)
        return new SliceImpl<>(list, pageable, hasNext);
    }
	
	
	
	
	// 가족 필터 적용
		@Override
		public Slice<Music> findAllFamilyList(String ownerName, String keyword, String sort, String searchType, Pageable pageable) {
			// 페이징
			int pageSize = pageable.getPageSize();
			int minRow = (int) pageable.getOffset();
			int maxRow = minRow + pageSize;
			
			// ownerName 처리 (JS에서 'all'이 넘어오면 전체 검색을 위해 null이나 특수값 처리 가능)
			// DB 쿼리에서 'all'일 때 처리를 하거나 여기서 분기
			String searchKeyword = (keyword == null) ? "" : keyword;
			String filterName = ownerName;
			if (ownerName == null || ownerName.isEmpty() || "all".equals(ownerName)) {
				filterName = "all"; // 반드시 문자열 "all"을 채워줘야 쿼리가 안 터집니다.
			} else if ("아빠".equals(ownerName)) {
				filterName = "father";
			} else if ("엄마".equals(ownerName)) {
				filterName = "mother";
			}else if ("형아".equals(ownerName)) {
				filterName = "dongheon";
			}else if ("동생".equals(ownerName)) {
				filterName = "juho";
			}
			
			// 새로운 Repository 메서드 호출 (가족용)
			List<Music> list = musicRepository.searchAllFamilyMusic(
					filterName,
					searchKeyword,
					sort,
					searchType,
					maxRow,
					minRow
			);
			
			boolean hasNext = list.size() == pageSize;
			return new SliceImpl<>(list, pageable, hasNext);
		}

		
		
		
// =====================================================================================================================	
// 2. 찾기	
// =====================================================================================================================	

	
	
	
	// 유저로 노래 찾기
	@Override
	public List<Music> findByName(Users user){
		return musicRepository.findByName(user);
	}
	
	
	// 음악 고유 아이디로 찾기(상세 정보)
	@Override
	public Music findById(int id){
		// 1. DB에서 데이터 조회 (id가 int이므로 .intValue() 사용)
        Music music = musicRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("해당 ID의 정보를 찾을 수 없습니다: " + id));

        // 2. 비즈니스 로직 처리 (예: 가사 데이터 가공)
        if (music.getLyrics() == null || music.getLyrics().isBlank()) {
            music.setLyrics("등록된 가사가 없습니다.");
        }

        // 3. 가공된 객체 반환
        return music;
	}
	
	
// =====================================================================================================================	
// 3. 업,다운로드 및 삭제	
// =====================================================================================================================	
	
//	// DB에 음악 업로드 저장
//	@Override
//	public Music insertFile(Music music) {
//		return musicRepository.save(music);
//	}
	
	// DB에 음악 업로드 저장
	@Override
	public Music insertFile(Music music) {
		// DB에 저장
		Music saveMusic = musicRepository.save(music);
		
		// 2. 엘라스틱서치 저장 (실패해도 전체 로직에 지장을 주지 않도록 처리)
	    try {
	        ElasticDocument document = ElasticDocument.from(saveMusic);
	        elasticRepository.save(document);
	    } catch (Exception e) {
	        // 로그만 남기고 실제 서비스는 계속 진행
	        log.error("Elasticsearch 동기화 실패 - Music ID: {}, Error: {}", saveMusic.getMusicId(), e.getMessage());
	    }
		return saveMusic;
	}
	
	
	
	
	// DB에 음악 데이터 삭제
	@Override
	@Transactional
	public Music deleteMusic(Music music) {
		// 서버에 저장되어 있는 파일 삭제
		if (music.getFilePath() != null) {
            File file = new File(music.getFilePath());
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("서버 파일 삭제 성공: " + music.getFilePath());
                } else {
                    System.err.println("서버 파일 삭제 실패: " + music.getFilePath());
                }
            }
        }
		
		musicRepository.delete(music);
		return music;
	}
	
// =====================================================================================================================	
// 4. 노래 변환
// =====================================================================================================================	

	
	// 노래 변환
	@Override
	@Transactional
	public Music musicConvert(Map<String, String> request, Users user) throws Exception{
		// 데이터 추출 및 유저 확인
		String youtube = request.get("videoUrl");
	    String artist = request.get("artist");
	    String title = request.get("title");
	    
	    Users member = memberRepository.findByKakaoId(user.getKakaoId())
	            .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));
	    System.out.println(member.getName());
	    
	    
	    // 2. 변환 API 호출
        String filePath = musicConvertClient.convertApi(member.getName(), youtube, title, artist);
        
        
        // 3. 파일 경로
        String fileName = title + "_" + artist + ".mp3"; 
        String fullPath = BASE_PATH + user.getName() + "/" + fileName;
        
        
        // 4. 실제 파일 존재 여부 검증
        File checkFile = new File(filePath); 
        
        if (!checkFile.exists() || !checkFile.isFile()) {
            // 파일이 없으면 예외를 던져서 DB 저장을 막고 컨트롤러로 실패 전달
            throw new RuntimeException("파일이 생성되지 않았습니다.");
        }
        
        // 5. 파일 크기가 0이면 실패로 간주
        if (checkFile.length() == 0) {
        	// 빈 파일은 삭제
        	checkFile.delete();
            throw new RuntimeException("파일 내용이 비어있습니다(0byte).");
        }
        
        
        // 6. DB 저장
        Music music = Music.builder()
                .title(title)
                .artist(artist)
                .filePath(fullPath)
                .name(user)
                .build();
        
        
        Music saveMusic = musicRepository.save(music);
        // 7. Elasticsearch 저장(자동완성용)
        // DB 저장이 성공했을 때만 실행(DB저장은 변환이 성공했을때만 저장하니)
        try {
            // DB 저장에 성공한 객체를 그대로 ES에 전달
            //elasticRepository.save(ElasticDocument.from(saveMusic));
        } catch (Exception e) {
            // ES 저장 실패가 전체 로직(파일 변환)을 취소시킬 필요는 없으므로 로그만 남김
        	log.error("음악 변환 중 에러 발생: {}", e.getMessage(), e);
        }
        
        
        return saveMusic;
		//return null;
	}
	
// =====================================================================================================================	
// 5. 기타 기능
// =====================================================================================================================	
	
	// 유니코드 변환
	public static String convertString(String val) {
		StringBuffer sb = new StringBuffer();
		
		for (int i=0;i<val.length();i++) {
			if('\\' == val.charAt(i) && 'u' == val.charAt(i+1)) {
				Character r = (char) Integer.parseInt(val.substring(i+2, i+6), 16);
				sb.append(r);
				i+=5;
			}else {
				sb.append(val.charAt(i));
				}
		}
		return sb.toString();
	}
	
// =====================================================================================================================	
// 6. 노래 수정
// =====================================================================================================================	
	

	
	@Override
	public Music updateMusicInfo(int musicId, Map<String, Object> request, Users user) {
		Music music = musicRepository.findById(musicId)
				.orElseThrow(() -> new RuntimeException("해당 곡을 찾을 수 없습니다."));
		
		music.setTitle((String) request.get("title"));
		music.setArtist((String) request.get("artist"));
		music.setLyrics((String) request.get("lyrics"));
		music.setAlbum((String) request.get("album"));
		music.setYear((String) request.get("year"));
		music.setGenre((String) request.get("genre"));
		return musicRepository.save(music);
	}

// =====================================================================================================================	
// 6. Elasticsearch
// =====================================================================================================================	
	
	@Override
	public List<String> getAutocompleteSuggestions(String keyword){
		
		System.out.println("여기 서비스로 넘어온 검색어: "+keyword);
		
		// 검색어가 없을 경우
		if(keyword == null || keyword.trim().isEmpty()) {
			return Collections.emptyList();
		}
		
		// 정렬 조건
		Sort sort = Sort.by(Sort.Direction.DESC, "musicId");
		String lowerKeyword = keyword.toLowerCase();
		
		
		// 10개만 가져옴
		PageRequest pageRequest = PageRequest.of(0,10, sort);
		List<ElasticDocument> docs = elasticRepository.findByTitleStartingWithOrArtistStartingWith(keyword, keyword, pageRequest);
		
		
//		List<String> resultList = new ArrayList<>();
//		
//		for (ElasticDocument doc : docs) {
//		    
//		    // 문서 하나에서 제목과 가수를 리스트로
//		    List<String> values = Arrays.asList(doc.getTitle(), doc.getArtist());
//		    
//		    //제목과 가수가 든 리스트를 다시 하나씩
//		    for (String text : values) {
//		        
//		        
//		        // null이 아니고, 검색어로 시작하는지 확인
//		        if (text != null && text.toLowerCase().startsWith(keyword)) {
//		            
//		            // 중복 제거
//		            if (!resultList.contains(text)) {
//		                resultList.add(text);
//		            }
//		        }
//		        
//		        // 개수 제한
//		        if (resultList.size() >= 10) {
//		            break; 
//		        }
//		    }
//		    if (resultList.size() >= 10) {
//		        break;
//		    }
//		}
//		return resultList;
		return docs.stream()
		        .flatMap(doc -> {
		            // 각각의 문서에서 제목과 가수를 뽑아 리스트로 만든 뒤 스트림으로 변환
		            // 이 방식이 타입 에러(infer type)를 피하는 가장 확실한 방법입니다.
		            List<String> values = Arrays.asList(doc.getTitle(), doc.getArtist());
		            return values.stream();
		        })
		        .filter(text -> text != null && text.toLowerCase().startsWith(keyword)) // 입력값으로 시작하는 단어만 필터링
		        .distinct() // ⭐ 중요: 중복 제거 (김범수 100번 나오지 않게)
		        .limit(10)  // 상위 10개만
		        .collect(Collectors.toList());
		
	
	}
	

	@Transactional(readOnly = true)
    @Override
    public void syncOracleToElastic() {
        List<Music> allMusic = musicRepository.findAll();
        List<ElasticDocument> docs = allMusic.stream()
                .map(ElasticDocument::from)
                .toList();
        elasticRepository.saveAll(docs);
    }
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
