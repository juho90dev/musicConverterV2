/* ==========================================================================
   1. 전역 상태 관리 (음악 목록 관련)
   ========================================================================== */
let currentPage = 0;
let isLastPage = false;
let isLoading = false;
let currentKeyword = ""; 
let currentSort = "music_id"; 
let currentSearchType = "all";
let currentFamily = "all"; // 초기값을 "all"로 설정r
let currentAudio = null;




/* ==========================================================================
   2. 초기화 및 이벤트 리스너 (DOMContentLoaded 대체)
   ========================================================================== */


function renderPage() {
	// 로컬스토리지에 저장된 사용자 이름 화면에 표시
	const nickname = localStorage.getItem('nickname');
	if (nickname) {
		document.getElementById('userNickname').innerText = nickname;
	}
	// 로그인 여부 체크
	// 보안을 위해 AccessToken이 없으면 메인페이지로 이동시키고 함수 실행 중단
	const token = localStorage.getItem('accessToken');
	if (!token) {
		alert("로그인이 필요한 페이지입니다.");
		window.location.href = "/";
		return;
	}
	// 3. 무한 스크롤 관찰자 설정 
	// 브라우저가 특정 요소(sentinel)를 감시하다가 화면에 보이면 특정 함수를 실행
    const observer = new IntersectionObserver((entries) => {
		// entries[0]: 감시 대상(sentinel)의 정보
        // .isIntersecting: 대상이 화면에 나타났는지 여부 (true/false)
        // '바닥(sentinel)이 보이며 로딩 중이 아니며 마지막 페이지가 아닐 때'만 다음 페이지 호출
        if (entries[0].isIntersecting && !isLoading && !isLastPage) {
            console.log("바닥 감지! 다음 페이지를 불러옵니다.");
            // 무한스크롤함수 호출
            loadMyMusicList();
        }
        // 10% 정도 보였을 때 실행
    }, { threshold: 0.1 });
	
	// 감시할 대상 요소를 찾아서 관찰 시작
    const sentinel = document.getElementById('sentinel');
    if (sentinel) observer.observe(sentinel);
    
	console.log("UI 렌더링 완료. 서버 연결은 컨트롤러 생성 후 활성화 예정.");
	
	// 페이지 진입 시 첫번째 페이지 즉시 호출
	loadMyMusicList();
	
	// 검색창 엔터키 활성화
	document.getElementById('searchInput').addEventListener('keypress', function(e) {
	    if (e.key === 'Enter') {
	        searchMusic();
	    }
	});
	
}

// 목록 불러오기 (무한 스크롤)
/*async function loadMyMusicList() {
    // 로딩 중이거나 마지막 페이지면 더 이상 요청하지 않음
    if (isLoading || isLastPage) return;
    
    isLoading = true;
    const spinner = document.getElementById('loading-spinner');
    if (spinner) spinner.classList.remove('d-none');

    try {
        console.log(`서버 요청: 페이지 ${currentPage}`);
        // 1. 페이지 번호와 사이즈를 쿼리 파라미터로 전달
        const url = `/api/list?page=${currentPage}&size=15&keyword=${encodeURIComponent(currentKeyword)}&sort=${currentSort}&searchType=${currentSearchType}`;
        const response = await authFetch(url);
        
        if (response.ok) {
        	
        	// Slice 또는 Page 객체 수신
            const data = await response.json(); 
            // 2. 서버 응답 데이터 구조에 따라 리스트 추출 (Slice 객체면 data.content)
            const list = data.content ? data.content : data;
            
            // 3. 데이터 그리기
            renderMusicCards(list);
            
            // 4. 마지막 페이지 여부 업데이트 (서버가 보내주는 last 필드 확인)
            isLastPage = data.last; 
            
            // 5. 다음 요청을 위해 페이지 번호 증가
            if (!isLastPage) {
                currentPage++;
            }
        } else {
            console.error("서버 응답 에러:", response.status);
        }
    } catch (err) {
        console.error("네트워크 오류:", err);
    } finally {
        isLoading = false;
        if (spinner) spinner.classList.add('d-none');
    }
}*/
/* ==========================================================================
   3. 무한 스크롤
   ========================================================================== */

// 무한 스크롤(Infinite Scroll)
async function loadMyMusicList() {
    // 로딩 중이거나 마지막 페이지면 더 이상 요청하지 않음
    //  -> 중복 요청 방지
    if (isLoading || isLastPage) return;
    
    // 검색 및 필터 값 추출
    currentSort = document.getElementById('sortSelect').value;
    currentSearchType = document.getElementById('searchType').value;
    currentKeyword = document.getElementById('searchInput').value;
    
    // 로딩 상태 시작
    // 데이터를 가져올때 상태를 로딩 중으로 변경
    isLoading = true;
    const spinner = document.getElementById('loading-spinner');
    if (spinner) spinner.classList.remove('d-none');

    try {
        console.log(`서버 요청: 페이지 ${currentPage}`);

        // 1. 현재 URL 경로 확인하여 API 주소 결정
        const isAllListPage = window.location.pathname.includes('alllist');
        // 주소에 따라 데이터를 요청할 서버 동적 변경
        const baseApiUrl = isAllListPage ? '/api/list/all' : '/api/list';

        // 쿼리 파라미터 구성
        // 페이지 번호/가져올 양(15개)/encodeURIComponent로 깨지지않도록 변환
        let url = `${baseApiUrl}?page=${currentPage}&size=15&keyword=${encodeURIComponent(currentKeyword)}&sort=${currentSort}&searchType=${currentSearchType}`;
        
        // 전체 리스트 페이지일 경우 '가족 멤버' 필터 파라미터 추가
        if (isAllListPage && currentFamily && currentFamily !== 'all') {
    		url += `&ownerName=${encodeURIComponent(currentFamily)}`;
		}
		
		// 인증정보를 포함하여 서버에 데이터 요청
        const response = await authFetch(url);
        
        console.log("실제 서버로 날아가는 주소:", url);
        // JSON 체크 추가(서버에서 에러가 발생해 html페이지를 돌려주면 오류가 난다.)
        const contentType = response.headers.get("content-type");
        if (!response.ok || !contentType || !contentType.includes("application/json")) {
            throw new Error("서버 응답이 올바른 데이터 형식이 아닙니다.");
        }

        if (response.ok) {
			// 서버에서 받은 문자열데이터를 자바스크립트 객체로 변환
            const data = await response.json();
            const list = data.content ? data.content : data;
            
            // 가져온 데이터를 html에 그려주는 함수 호출
            renderMusicCards(list);
            
            // 마지막 페이지 여부 업데이트
            isLastPage = data.last; 
            
            // 마지막 페이지가 아니면 페이지 번호 1증가
            if (!isLastPage) {
                currentPage++;
            }
        } else {
            console.error("서버 응답 에러:", response.status);
        }
    } catch (err) {
        console.error("네트워크 또는 데이터 처리 오류:", err);
    } finally {
		// 성공여부와 상관없이 무조건 실행
		// 로딩 상태를 false로 변환 후 로딩 스피너 숨김
        isLoading = false;
        if (spinner) spinner.classList.add('d-none');
    }
}


/* ==========================================================================
   3. 검색 기능
   ========================================================================== */


// 초기 설정
const searchInput = document.getElementById('searchInput');
const autocompleteResults = document.getElementById('autocomplete-results');
const searchType = document.getElementById('searchType');
const musicListContainer = document.getElementById('music-list-container');
const resultsContainer = document.getElementById('autocomplete-results');

let timer;
let nowIndex = -1;
let matchDataList = [];
let isSelecting = false;
let debounceTimer;



/* ==========================================================================
   4. 엘라스틱서치 자동완성 
   ========================================================================== */

// 입력 이벤트 리스너
if (searchInput) {
	searchInput.addEventListener('input', (e) => {
		
		if (isSelecting) return;
		
	    const keyword = e.target.value;
	
	    // 이전 타이머를 취소함 (글자를 계속 치고 있으면 실행 안 됨)
	    clearTimeout(timer);
	
	    // 200ms(0.2초) 후에 실행되도록 예약
	    timer = setTimeout(() => {
			if (isSelecting) return;
	        if (keyword.trim().length > 0) {
	            console.log("이제 서버로 전송합니다:", keyword);
	            autoComplete(keyword);
	        }else {
	            resultsContainer.classList.add('d-none');
	        }
	    }, 200);
	});
}
// 서버 통신
async function autoComplete(keyword) {
    const token = localStorage.getItem('accessToken'); 

    try {
        const response = await fetch(`/api/search/autocomplete?keyword=${encodeURIComponent(keyword)}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.status === 401) {
            console.error("토큰이 만료되었거나 권한이 없습니다.");
            return;
        }

        const data = await response.json();
        displayResults(data);
        console.log("인증 후 받은 결과:", data);
        
        
    } catch (error) {
        console.error("데이터 로드 실패:", error);
    }
}


// 결과 출력
function displayResults(data) {
    const autocompleteResults = document.getElementById('autocomplete-results');
    const searchInput = document.getElementById('searchInput');
    if (!autocompleteResults || !searchInput) return;
    
    // 현재 리스트를 전역 변수에 저장 (키보드 조작용)
    matchDataList = data; 
    nowIndex = -1; // 리스트가 새로 뜨면 선택 위치 초기화
    
    
    if (data.length === 0) {
        autocompleteResults.classList.add('d-none');
        return;
    }
    
    const query = searchInput.value.trim(); // 사용자가 입력한 검색어

    autocompleteResults.innerHTML = data.map(text => {
        // 1. [보안/에러방지] 홑따옴표와 쌍따옴표 치환
        const escapedText = text.replace(/'/g, "\\'").replace(/"/g, "&quot;");
        
        // 2. [RegExp 하이라이트] 입력한 글자만 <b> 태그로 감싸기
        let highlightedText = text;
        if (query) {
            // 특수문자 탈출 및 정규식 생성 (gi: 전체검색, 대소문자 무시)
            const escapedQuery = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const regex = new RegExp(`(${escapedQuery})`, 'gi');
            // 일치하는 부분을 클래스 있는 b 태그로 변경 (공백 없이 붙임)
            highlightedText = text.replace(regex, `<b class="highlight">$1</b>`);
        }
        
        // 3. [HTML 생성] <span> 태그 안에 하이라이트된 텍스트 삽입
        return `
            <div class="autocomplete-item" 
                 onmousedown="event.preventDefault(); selectResult('${escapedText}')">
                <i class="bi bi-search"></i>
                <span>${highlightedText}</span>
            </div>
        `;
    }).join('');
    
    autocompleteResults.classList.remove('d-none');
}

// 자동완성된 추천 리스트 중 선택
function selectResult(item) {
	const searchInput = document.getElementById('searchInput');
    const resultsContainer = document.getElementById('autocomplete-results');
    
    clearTimeout(timer);
	isSelecting = true;
	
    // 1. 검색창에 선택한 값 넣기 (item이 객체라면 item.title 등으로 수정)
    searchInput.value = item; 
    
    // 2. 드롭다운 숨기기
    resultsContainer.innerHTML = '';
    resultsContainer.classList.add('d-none');
    
    // 3. (선택사항) 바로 검색 결과 페이지로 이동하거나 검색 함수 실행
    console.log("선택된 키워드로 검색을 시작합니다:", item);
    setTimeout(() => { isSelecting = false; }, 100);
    searchMusic(); 
}

if (searchInput) {
	// 키보드 조작 (방향키, 엔터)
	searchInput.addEventListener('keydown', (e) => {
	    const items = resultsContainer.querySelectorAll('.autocomplete-item');
	    
	    // 방향키나 엔터가 아니면 조작 해제
	    if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp' && e.key !== 'Enter') {
	    	// 이제 다시 자동완성 검색 허용
	        isSelecting = false; 
	    }
	    
	    // 리스트가 없거나 닫혀있으면 무
	    if (items.length === 0 || resultsContainer.classList.contains('d-none')) return;
	
	    // 한글 조합 중 엔터를 쳤을 때 중복 입력되는 현상 방지
	    if (e.isComposing || e.keyCode === 229) return;
	
	    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
	        e.preventDefault(); 
	        isSelecting = true; 
	
	        if (e.key === 'ArrowDown') {
	            nowIndex = (nowIndex + 1) % items.length;
	        } else {
	            nowIndex = (nowIndex <= 0) ? items.length - 1 : nowIndex - 1;
	        }
	        updateFocus(items);
	        if (matchDataList[nowIndex]) {
	            // 이 코드가 실행되면 input 이벤트가 발생하지만, 
	            // isSelecting이 true라 서버 요청을 안 보낼 겁니다.
	            searchInput.value = matchDataList[nowIndex];
	        }
	    } else if (e.key === 'Enter') {
	        if (nowIndex > -1) {
	            e.preventDefault();
	            selectResult(matchDataList[nowIndex]);
	            searchMusic();
	        }
	    }
	});
}
function updateFocus(items) {
    // 모든 항목에서 active 클래스 제거
    items.forEach(item => item.classList.remove('active'));

    if (nowIndex > -1 && items[nowIndex]) {
        // 현재 인덱스 항목에 active 클래스 추가
        items[nowIndex].classList.add('active');
        
        // 스크롤이 있는 경우 선택된 항목이 보이게 조절
        items[nowIndex].scrollIntoView({ block: 'nearest' });
    }
}


async function searchMusic() {
    clearTimeout(debounceTimer);
    hideAutocomplete();

    const keyword = searchInput.value.trim();
    if (!keyword) return;

    currentKeyword = keyword;
    
    currentPage = 0;
    isLastPage = false;
    const container = document.getElementById('music-list-container');
    if (container) container.innerHTML = "";
    
    console.log("메인 검색 실행:", currentKeyword);
    await loadMyMusicList(); 
}
function hideAutocomplete() {
    const autocompleteResults = document.getElementById('autocomplete-results');
    if (autocompleteResults) autocompleteResults.classList.add('d-none');
    matchDataList = [];
    nowIndex = -1;
}

document.addEventListener('mousedown', (e) => {
    if (!searchInput.contains(e.target) && !resultsContainer.contains(e.target)) {
        resultsContainer.classList.add('d-none');
    }
});


const clearSearchBtn = document.getElementById('clearSearch');
if (searchInput) {
	// 입력 시 X 버튼 보이기/숨기기
	searchInput.addEventListener('input', (e) => {
	    if (e.target.value.length > 0) {
	        clearSearchBtn.style.display = 'block';
	    } else {
	        clearSearchBtn.style.display = 'none';
	    }
	});
}
if (clearSearchBtn && searchInput) {
	// X 버튼 클릭 시 초기화
	clearSearchBtn.addEventListener('click', () => {
	    searchInput.value = '';
	    clearSearchBtn.style.display = 'none';
	    hideAutocomplete(); // 기존에 만드신 자동완성 숨기기 함수
	    isSelecting = false; // 가드 해제
	    searchInput.focus(); // 다시 입력할 수 있게 포커스
	});
}


/* ==========================================================================
   5. 정렬
   ========================================================================== */

// 정렬 변경
async function changeSort() {
	// 선택된 정렬 값 추출
	// 선택한 값(최신순, 인기순 등)을 가져와 전역 변수에 저장
    currentSort = document.getElementById('sortSelect').value;
    
    // 초기화
    // 정렬 기준이 바뀌면 데이터의 순서가 달라지니 1페이지부터 다시 받아와야한다.
    currentPage = 0;
    
    // 마지막 페이지도 초기화
    isLastPage = false;
    
    // 기존에 출력된 음악 리스트 초기화
    document.getElementById('music-list-container').innerHTML = ""; 
    
    console.log("정렬 변경:", currentSort);
    
    // 데이터 재요청
    await loadMyMusicList();
}



/* ==========================================================================
   6. 다운로드 
   ========================================================================== */

// 다운로드 기능
async function downloadMusic(filePath, title, artist) {
	// 단순히 css부분임
	// 화면 우측 상단에 나타났다 사라지는 알림창 설정
    const Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        // 확인버튼 없음
        showConfirmButton: false,arguments,
        // 2초 뒤 자동 종료
        timer: 2000
    });
    Toast.fire({ icon: 'info', title: '다운로드를 요청 중입니다...' });


	
    try {
        // 서버에 파일 데이터 요청
        // authFetch를 통해 인증 토큰을 담아 POST방식으로 서버에 요청(로그인 한 사람만 파일 다운로드할 수 있도록 제한)
        const response = await authFetch('/api/music/fileDownload', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            // 가수/제목/파일경로를 JSON 형태로 서버에 전달
            body: JSON.stringify({
                artist: artist,
                title: title,
                filePath: filePath
            })
        });
		
		// 서버 응답이 실패한 경우 에러메시지 및 중단
        if (!response.ok){
        	alert("다운로드 권한이 없거나 파일이 없습니다.");
        	return;
        }

        // 서버 응답을 Blob(바이너리 데이터)의로 변환
        // *참고 Blob(Binary Large Object)은 이미지, 사운드 같은 이진 데이터를 다루는 객체
        const blob = await response.blob();
        
        // 브라우저 메모리에 가상 URL 생성
        // 내 컴퓨터 메모리에 있는 이 데이터를 가리키는 임시 주소(blob:http://...)
        const url = window.URL.createObjectURL(blob);
        
        // 가상의 <a>태그를 생성
        const a = document.createElement('a');
        a.href = url;
        
        // 파일명 설정 (가수-제목.mp3)
        a.download = `${artist}-${title}.mp3`;
        
        // 가상 링크를 HTML 바디에 잠시 추가하고 클릭 이벤트를 발생시켜 다운로드를 실행
        document.body.appendChild(a);
        // 다운로드 실행
        a.click(); 
        
        // 메모리 정리
        // 사용이 끝난 임시 URL을 해제하여 메모리 누수를 방지하고 가상 태그를 삭제
        window.URL.revokeObjectURL(url);
        a.remove();
        
        console.log("다운로드 완료:", title);
        
    } catch (error) {
		// 네트워크 연결 실패 등 예외 상황 처리
        console.error("Download Error:", error);
        Swal.fire({
            title: '오류 발생',
            text: '다운로드 중 문제가 발생했습니다.',
            icon: 'error',
            width: '300px'
        });
    }

    
}


/* ==========================================================================
   7. UI 렌더링 (화면 그리기)
   ========================================================================== */



function renderMusicCards(musicList) {
    const container = document.getElementById('music-list-container');
    
    // 데이터가 없을 때 처리
    // 첫 페이지(currentPage === 0)인데 데이터가 하나도 없다면 "비어있습니다" 메시지를 출력
    if (currentPage === 0 && (!musicList || musicList.length === 0)) {
        container.innerHTML = `<div class="col-12 text-center py-5 text-muted">음악 목록이 비어있습니다.</div>`;
        return;
    }
    
    // 현재 페이지가 '전체 리스트'인지 확인
    const isAllListPage = window.location.pathname.includes('alllist');
    
    // 리스트를 반복하며 카드 생성
    musicList.forEach(music => {
    	// 작은따옴표(')가 포함된 제목/가수명을 위해 escape 처리(js 문법 에러 방지)
        const safeTitle = music.title.replace(/'/g, "\\'");
        const safeArtist = (music.artist || "Unknown").replace(/'/g, "\\'");
    	
    	// 가족 이름 가져오기
    	const ownerName = music.name ? getMemberDisplayName(music.name.name) : '가족';
    	
    	// 페이지 타입에 따른 버튼 변경
    	let actionAreaHtml = "";
        if (isAllListPage) {
            // 가족 페이지: 삭제 버튼 대신 소유자 이름
            actionAreaHtml = `
                <div class="text-end ms-2" style="min-width: 50px;">
                    <span class="owner-badge">${ownerName}</span>
                </div>`;
        } else {
            // 내 노래 페이지: 삭제 버튼
            actionAreaHtml = `
                <button class="btn action-btn" onclick="deleteMusic(${music.musicId}, this)">
                    <i class="bi bi-x-lg" style="font-size: 1.1rem;"></i>
                </button>`;
        }
    	
    	
    	// 실제 카드 html 구조 생성
        const cardHtml = `
            <div class="col-12">
                <div class="card history-card p-2">
                    <div class="d-flex align-items-center">
                        <img src="${music.albumCover || '/asset/img/favicon.ico'}" class="album-art me-3">
                        <div class="flex-grow-1 overflow-hidden" style="cursor: pointer;" onclick="musicInfo(${music.musicId})">
                            <h6 class="mb-0 text-truncate" style="font-size: 1rem; font-weight: 500; letter-spacing: -0.3px;">
                                ${music.title}
                            </h6>
                            <p class="text-muted mb-0 small text-truncate" style="font-size: 0.85rem; opacity: 0.8;">
                                ${music.artist}
                            </p>
                        </div>
                        <div class="d-flex align-items-center ms-2">
                            <button id="play-btn-${music.musicId}" class="btn action-btn text-primary" onclick="playMusic('${music.musicId}', '${safeTitle}', '${safeArtist}')">
                                <i class="bi bi-play-fill" style="font-size: 1.5rem;"></i>
                            </button>
                            <button class="btn action-btn" onclick="downloadMusic('${music.filePath}', '${safeTitle}', '${safeArtist}')">
                                <i class="bi bi-download" style="font-size: 1.1rem;"></i>
                            </button>
                            ${actionAreaHtml}
                        </div>
                    </div>
                </div>
            </div>
        `;
        // 기존 목록 '뒤에' 추가
        // container.innerHTML = ... 을 쓰면 기존 내용이 덮어씌워진다.
        // 무한 스크롤이니 이어서 붙여야 한다.
        container.insertAdjacentHTML('beforeend', cardHtml);
    });
}
// 영문 이름을 한글로 매핑
function getMemberDisplayName(dbName) {
    const memberMap = {
        'father': '아빠',
        'mother': '엄마',
        'dongheon': '동헌',
        'juho': '주호',
        'all': '전체'
    };
    return memberMap[dbName] || dbName;
}
let currentPlayingId = null; // 현재 재생 중인 곡의 ID


/* ==========================================================================
   8. 노래 재생
   ========================================================================== */



async function playMusic(musicId, title, artist) {
    // 1. 같은 곡 클릭 시 (토글)
    if (currentAudio && currentPlayingId === musicId) {
        if (!currentAudio.paused) {
            currentAudio.pause();
            toggleIcon(musicId, false); // 일시정지 아이콘 -> 재생 아이콘
        } else {
            currentAudio.play();
            toggleIcon(musicId, true); // 재생 아이콘 -> 일시정지 아이콘
        }
        return;
    }

    // 2. 다른 곡 재생 시 이전 곡 초기화
    if (currentAudio) {
        currentAudio.pause();
        toggleIcon(currentPlayingId, false);
    }

    // 3. 새 곡 재생 시작
    const streamUrl = `/api/music/stream/${musicId}`;
    currentAudio = new Audio(streamUrl);
    currentPlayingId = musicId;

    currentAudio.play()
        .then(() => {
            toggleIcon(musicId, true);
        })
        .catch(e => {
            console.error("재생 에러:", e);
            // 401 에러 등이 나면 여기서 알림
            Swal.fire({ icon: 'error', title: '재생 실패', text: '인증 문제나 파일이 없습니다.', timer: 1500 });
        });

    // 4. 노래 종료 시 아이콘 원복
    currentAudio.onended = () => {
        toggleIcon(musicId, false);
        currentPlayingId = null;
        currentAudio = null;
    }
}
// 아이콘 변경 전용 함수
function toggleIcon(musicId, isPlaying) {
    const btn = document.getElementById(`play-btn-${musicId}`);
    if (!btn) return;

    const icon = btn.querySelector('i');
    if (isPlaying) {
        icon.classList.replace('bi-play-fill', 'bi-pause-fill');
        btn.classList.replace('text-primary', 'text-danger'); // 재생 중일 때 빨간색으로 강조 (선택사항)
    } else {
        icon.classList.replace('bi-pause-fill', 'bi-play-fill');
        btn.classList.replace('text-danger', 'text-primary'); // 다시 원래 파란색으로
    }
}




/* ==========================================================================
   9. 노래 삭제
   ========================================================================== */
   
   
// 노래 삭제
async function deleteMusic(musicId, element) {
    // 1. 확인 창 띄우기
    const result = await Swal.fire({
        title: '삭제하시겠습니까?',
        text: "목록에서 이 곡이 사라지며, 복구할 수 없습니다.",
        icon: 'warning',
        width: '300px', // 너비 축소
        showCancelButton: true,
        confirmButtonColor: '#764ba2', // 서비스 테마색
        cancelButtonColor: '#aaa',
        confirmButtonText: '삭제',
        cancelButtonText: '취소',
        reverseButtons: true // 확인/취소 버튼 위치 반전 (취소가 왼쪽)
    });
	
	console.log("실제 서버 삭제 요청 보낼 ID:", musicId);
    // 2. 사용자가 '삭제'를 눌렀을 때만 실행
    if (result.isConfirmed) {
		try{
			
			console.log("실제 서버 삭제 요청 보낼 ID:", musicId);

			const deleteUrl = `/api/music/${musicId}`;
			console.log("최종 요청 주소:", deleteUrl); //
			// 서버에 삭제 요청
			const response = await authFetch(`/api/music/${musicId}`, {
				method: 'DELETE'
			});
			if(response.ok){
				// 서버에서 삭제 성공 시에만 화면에서 요소 제거
        		element.closest('.col-12').remove();
        		
        		// 만약 이게 마지막 곡이었다면?
			    const container = document.getElementById('music-list-container');
			    // .history-card 개수를 세서 0개면 메시지 출력
			    if (container.querySelectorAll('.history-card').length === 0) {
			        container.innerHTML = `<div class="col-12 text-center py-5 text-muted">음악 목록이 비어있습니다.</div>`;
			    }
        		
        		
				// 삭제 완료 알림
	        	Swal.fire({
		            title: '삭제되었습니다.',
		            icon: 'success',
		            width: '250px',
		            timer: 1000,
		            showConfirmButton: false
	        	});
			}else{
				const errorData = await response.json();
                Swal.fire('실패', errorData.message || '삭제 권한이 없습니다.', 'error');
			}
		}catch(err){
			console.error("삭제 중 오류 발생:", err);
            Swal.fire('오류', '서버와 통신할 수 없습니다.', 'error');
		}
        
    }
}



/* ==========================================================================
   10. 상세 페이지 이동
   ========================================================================== */
// 상세 페이지 이동
function musicInfo(musicId) {
    
    window.location.href = `/music/musicinfo?id=${musicId}`;
	
}










