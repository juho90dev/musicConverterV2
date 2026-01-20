package com.jh.convert.music.client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MusicConvertClient {
	
	// 1. 보안 설정 / 허용된 내부 API 도메인 (SSRF 방어용 화이트리스트)
	@Value("${server-config.allowed-host}")
    private  String allowedHost;
	@Value("${server-config.allowed-port}")
    private int allowedPort;
    
    
    private static final Pattern YOUTUBE_URL_PATTERN = 
            Pattern.compile("^(https?://)?(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/)[a-zA-Z0-9_-]{11}([?&].*)?$");
	
    
    public String convertApi(String user, String youtube, String title, String artist) throws Exception{
    	
    	// 2. 입력 데이터 검증 (SSRF 방어)
        if (youtube == null || !YOUTUBE_URL_PATTERN.matcher(youtube).matches()) {
            throw new IllegalArgumentException("유효하지 않은 YouTube URL 형식입니다.");
        }
    	
    	try {
    		// API_ENDPOINT 조립
    		String apiEndpoint = "http://" + allowedHost + ":" + allowedPort + "/api/mp3";
    		
    		// URL 파라미터 인코딩 및 생성
    		String urlStr = String.format("%s?user=%s&url=%s&title=%s&artist=%s", apiEndpoint,
                    URLEncoder.encode(user, "UTF-8"),
                    URLEncoder.encode(youtube, "UTF-8"),
                    URLEncoder.encode(title, "UTF-8"),
                    URLEncoder.encode(artist, "UTF-8"));
    		
    		URL url = new URL(urlStr);
    		
    		// 3. SSRF 방어: 최종 호출 전 호스트와 포트가 변조되지 않았는지 재확인
            if (!url.getHost().equals(allowedHost) || url.getPort() != allowedPort) {
                throw new SecurityException("허용되지 않은 도메인으로의 접근이 차단되었습니다.");
            }
    		
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(5000); 
            urlConnection.setReadTimeout(30000);
            
            // 연결 확인용 로그
            System.out.println("요청 URL: " + urlStr);
            int res = urlConnection.getResponseCode();
            
            if (res == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n\r");
                    }
                    // 4. 받은 데이터 가공 (따옴표 제거 -> 유니코드 변환 -> 공백 제거)
                    String rawData = sb.toString().replaceAll("\"", "").trim();
                    
                    // 유니코드 변환 로직 호출
                    String filePath = convertUnicode(rawData);
                    
                    // 최종 공백 제거 후 반환
                    return filePath.stripTrailing();
                }
            } else {
                // 서버로부터 에러 상태 코드를 받은 경우
                throw new RuntimeException("변환 서버 에러 (상태 코드: " + res + ")");
            }
    	}catch (java.net.ConnectException e) {
            throw new Exception("변환 서버(9991)가 꺼져 있거나 연결이 거부되었습니다.");
        } catch (java.net.SocketTimeoutException e) {
            throw new Exception("변환 서버 응답 시간이 초과되었습니다. (서버 부하 확인 필요)");
        } catch (Exception e) {
            throw new Exception("통신 오류: " + e.getMessage());
        }
    }
	
	// 유니코드 변환 로직
    private String convertUnicode(String val) {
        if (val == null || !val.contains("\\u")) return val;
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < val.length(); i++) {
            if (i <= val.length() - 6 && '\\' == val.charAt(i) && 'u' == val.charAt(i + 1)) {
                try {
                    String hex = val.substring(i + 2, i + 6);
                    char r = (char) Integer.parseInt(hex, 16);
                    sb.append(r);
                    i += 5;
                } catch (NumberFormatException e) {
                    sb.append(val.charAt(i));
                }
            } else {
                sb.append(val.charAt(i));
            }
        }
        return sb.toString();
    }
    
    
    

}
