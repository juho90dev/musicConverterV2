package com.jh.convert.music.model.vo;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;




import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Document(indexName = "music", createIndex = false)
public class ElasticDocument {
	
	
	@Id
    private String id; // ES 내부 ID (String)
	
	// Oracle DB의 PK와 매핑
	//@Field(type = FieldType.Long)
	private int musicId;
	
	
	//@Field(type = FieldType.Text, analyzer = "music-nori-analyzer")
	private String title;
	
	//@Field(type = FieldType.Text, analyzer = "music-nori-analyzer")
	private String artist;
	
	public static ElasticDocument from(Music music) {
		ElasticDocument doc = new ElasticDocument();
		doc.id = String.valueOf(music.getMusicId());
		doc.musicId = music.getMusicId();
		doc.title = music.getTitle();
		doc.artist = music.getArtist();
		return doc;
	}
}
