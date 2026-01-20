package com.jh.convert.music.model.dao;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.jh.convert.music.model.vo.ElasticDocument;


@Repository
public interface ElasticRepository extends ElasticsearchRepository<ElasticDocument, String>{
	// 자동완성용: 제목 또는 가수가 입력어로 시작하는 데이터 찾기
	// tartsWith를 사용하면 '아이' 입력 시 '아이유', '아이야' , '아이고' 등이 검색된다
	/*
	 * @Query("{\"bool\": {\"should\": [" +
	 * "{\"match\": {\"title.autocomplete\": \"?0\"}}," +
	 * "{\"match\": {\"artist.autocomplete\": \"?1\"}}" + "]}}")
	 * List<ElasticDocument> findByTitleStartsWithOrArtistStartsWith(String title,
	 * String artist, Sort sort);
	 */
	
	List<ElasticDocument> findByTitleContainingOrArtistContaining(String title, String artist, Sort sort);
	
	
	@Query("{\"bool\": {\"should\": [" +
		       "{\"match\": {\"title.autocomplete\": \"?0\"}}," +
		       "{\"match\": {\"artist.autocomplete\": \"?1\"}}" +
		       "]}}")
    List<ElasticDocument> findByTitleStartingWithOrArtistStartingWith(String title, String artist, Pageable pageable);
//	@Query("{" +
//			"  \"bool\": {" +
//			"    \"should\": [" +
//			"      { \"prefix\": { \"title.keyword\": \"?0\" } }," +
//			"      { \"prefix\": { \"artist.keyword\": \"?1\" } }" +
//			"    ]" +
//			"  }" +
//			"}")
//	List<ElasticDocument> findByTitleStartingWithOrArtistStartingWith(String title, String artist, Pageable pageable);

}

