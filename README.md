# musicConverter-V2
YouTube 영상을 MP3 파일로 변환하고 저장하며, 저장된 파일을 언제든 다운로드하여 재생할 수 있는 웹 애플리케이션

### Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.4.1 (Maven)
- **Database**: Oracle Database, Redis, Elasticsearch (Search)
- **Security**: Spring Security, OAuth 2.0 (Kakao), JWT
- **View**: Thymeleaf

---

##  Roadmap & Updates

### ver 2.1
- [x] **OAuth 2.0 Integration**: Kakao Login API를 통한 소셜 로그인 구현
- [x] **JWT Auth System**:
  - jjwt 0.12.3을 이용한 Stateless 인증 체계
  - Access/Refresh Token 발급 및 유효성 검증 필터 구현
- [x] **Redis Management**: Spring Data Redis를 활용한 Refresh Token 저장 및 로그아웃 토큰 처리

### ver 2.2
- [x] **Up/Download**: 음악 파일 업로드 및 다운로드 기능
- [x] **Streaming**: 웹 브라우저 내 실시간 음원 재생 서비스 기능
- [x] **Data Consistency**: DB 데이터 삭제 시 물리적 파일 시스템 내 음원 자동 삭제
- [ ] **YouTube to MP3**: 외부 API 연동을 통한 실시간 음원 변환 및 저장

### ver 2.3
- [x] **Infinity Scroll**:
  - 음원 리스트 조회 시 페이지 무한 스크롤 기능 구현
  - `Spring Data JPA/JDBC`의 Pageable을 활용한 페이징 처리

### ver 2.4
- [x] **Search**:
  - Elasticsearch를 활용한 가수명/제목 형태소 분석 및 초고속 검색 기능

### ver 2.5
- [ ] **Metadata Management (MP3Tag)**:
  - 변환된 MP3 파일에서 ID3 Tag(제목, 아티스트, 앨범 아트 등) 추출 및 분석
  - 추출된 메타데이터를 기반으로 DB 및 Elasticsearch 인덱스 자동 업데이트
  - 다운로드 시 사용자 기기에서 메타데이터가 정상 표시되도록 보정
