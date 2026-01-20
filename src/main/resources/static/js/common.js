
// 인증
/*async function authFetch(url, options = {}) {
	let accessToken = localStorage.getItem('accessToken');
	
	// 헤더 초기화 및 Access Token 추가
	options.headers = {
		...options.headers,
		'Authorization': `Bearer ${accessToken}`,
		'Content-Type': 'application/json'
	};
	
	// Body가 FormData(업로드 등)가 아닐 경우에만 JSON 설정
    if (options.body && !(options.body instanceof FormData)) {
        options.headers['Content-Type'] = 'application/json';
    }
	
	
	// 1단계: API 요청 시도
	let response = await fetch(url, options);
	
	// 2단계: 만약 401(만료) 에러가 발생했다면 재발급 시도
	if (response.status === 401) {
		console.log("Access Token 만료. 재발급 시도 중...");
		const refreshToken = localStorage.getItem('refreshToken');
		
		if (!refreshToken) {
			handleLogout();
			return;
		}
		
		// 서버에 Refresh Token을 보내 새 Access Token 요청
		const refreshRes = await fetch('/api/auth/refresh', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ refreshToken })
		});
		
		if (refreshRes.ok) {
			const data = await refreshRes.json();
			// 새 토큰 저장
			localStorage.setItem('accessToken', data.accessToken);
			
			// 3단계: 원래 요청 재시도 (새 토큰으로 교체)
			options.headers['Authorization'] = `Bearer ${data.accessToken}`;
			return await fetch(url, options);
		} else {
			// Refresh Token도 만료된 경우
			handleLogout(true);
		}
	}
	return response;
}*/
async function authFetch(url, options = {}) {
    let accessToken = localStorage.getItem('accessToken');
    
    // 1. 헤더 객체 안전하게 생성
    const headers = { 
		...options.headers 
	};
    if (accessToken) {
        headers['Authorization'] = `Bearer ${accessToken}`;
    }

    // body가 FormData가 아닐 때만 JSON 헤더를 추가
    if (options.body && !(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }

    options.headers = headers;
    // 1단계: API 요청 시도
    let response = await fetch(url, options);

    // 2단계: 401 에러(만료) 처리
    if (response.status === 401) {
        console.log("Access Token 만료. 재발급 시도 중...");
        const refreshToken = localStorage.getItem('refreshToken');

        if (!refreshToken) {
            handleLogout();
            // undefined 방지를 위해 기존 응답 반환
            return response; 
        }

        const refreshRes = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });
		console.log("재발급 응답 상태코드:", refreshRes.status);
        if (refreshRes.ok) {
            const data = await refreshRes.json();
            localStorage.setItem('accessToken', data.accessToken);

            // 3단계: 새 토큰으로 헤더 교체 후 재시도
            options.headers['Authorization'] = `Bearer ${data.accessToken}`;
            return await fetch(url, options); 
        } else {
            handleLogout(true);
            return refreshRes; // 실패 응답이라도 반환해야 .ok 체크에서 에러 안 남
        }
    }
    return response;
}

// 로그아웃 함수
/*async function handleLogout(isAuto = false) {
	
	const Toast = Swal.mixin({
        toast: true,
        position: 'top',
        showConfirmButton: false,
        timer: 2000,
        width: '400px',
        timerProgressBar: true
    });
    // 1. 자동 로그아웃이 아닐 경우에만 확인창 띄우기
    if (!isAuto) {
        const result = await Swal.fire({
            toast: true,
            position: 'top',
            width: '400px',
            icon: 'question',
            title: '로그아웃 하시겠습니까?',
            showConfirmButton: true,
            showCancelButton: true,
            confirmButtonText: '확인',
            cancelButtonText: '취소',
            confirmButtonColor: '#764ba2'
        });
        
        if (!result.isConfirmed) return;
    }

    const token = localStorage.getItem('accessToken');

	// 서버 통신 시도 (실패해도 클라이언트 로그아웃은 진행)
    try {
        // 2. 서버에 로그아웃 요청 (Redis 삭제)
        const response = await fetch('/logout', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + token
            }
        });

        if (response.ok) {
            console.log("서버 로그아웃 및 Redis 삭제 성공");
        } else {
            console.error("서버 로그아웃 처리 중 오류 발생 (상태코드: " + response.status + ")");
        }
    } catch (e) {
        console.error("서버 통신 중 에러 발생:", e);
    } finally {
        // 3. 서버 응답 여부와 상관없이 클라이언트 정보는 무조건 삭제
        localStorage.clear();
        
        if (isAuto) {
            alert("약 30분동안 활동이 없어 자동 로그아웃합니다.");
            // 3. 자동 로그아웃 알림 (중앙 상단)
            // await를 붙여야 토스트가 유지되는 동안 페이지 이동을 잠시 대기합니다.
            await Toast.fire({
                icon: 'warning',
                title: '30분간 활동이 없어 자동 로그아웃되었습니다.'
            });
            location.href = "/";
            
        }
        
        // 4. 메인 페이지로 이동 (이제 주석 해제!)
        location.href = "/";
    }
}*/
async function handleLogout(isAuto = false) {
    // 1. 자동 로그아웃이 아닐 경우에만 확인창 띄우기
    if (!isAuto) {
        const result = await Swal.fire({
            toast: true,
            position: 'top',
            width: '400px',
            icon: 'question',
            title: '로그아웃 하시겠습니까?',
            showConfirmButton: true,
            showCancelButton: true,
            confirmButtonText: '확인',
            cancelButtonText: '취소',
            confirmButtonColor: '#764ba2'
        });
        
        if (!result.isConfirmed) return;
    }

    const token = localStorage.getItem('accessToken');

	// 서버 통신 시도 (실패해도 클라이언트 로그아웃은 진행)
    try {
        // 2. 서버에 로그아웃 요청 (Redis 삭제)
        const response = await fetch('/logout', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            console.log("서버 로그아웃 및 Redis 삭제 성공");
        } else {
            console.error("서버 로그아웃 처리 중 오류 발생 (상태코드: " + response.status + ")");
        }
    } catch (e) {
        console.error("서버 통신 중 에러 발생:", e);
    } finally {
        // 3. 서버 응답 여부와 상관없이 클라이언트 정보는 무조건 삭제
        localStorage.clear();
        
        if (isAuto) {
            // 자동 로그아웃: 알림창 띄우자마자 바로 이동
           await Swal.fire({
        icon: 'warning',
        title: '자동 로그아웃 안내',
        text: '장시간 활동이 없어 안전하게 로그아웃되었습니다.',
        position: 'center',
        width: '90%', 
        // maxWidth와 borderRadius를 여기서 지웁니다.
        
        showConfirmButton: true,
        confirmButtonText: '확인',
        confirmButtonColor: '#764ba2',
        
        allowOutsideClick: false,
        
        
        didOpen: (popup) => {
            popup.style.maxWidth = '400px';
            popup.style.borderRadius = '15px';
        },
        
        customClass: {
            popup: 'shadow-lg',
            title: 'fw-bold'
        }
    });
            location.href = "/";
        } else {
            // 수동 로그아웃: 성공 알림 없이(혹은 아주 짧게) 바로 메인으로
            location.href = "/";
        }
    }
}

// 헤더 UI 업데이트 함수
function refreshHeaderUI() {
    const token = localStorage.getItem('accessToken');
    const nickname = localStorage.getItem('nickname');
    const userMenu = document.getElementById('user-menu');
    const nicknameSpan = document.getElementById('user-nickname');
        
    if (token && userMenu) {
        userMenu.classList.remove('d-none');
        if (nicknameSpan && nickname) {
            // nicknameSpan.innerText = decodeURIComponent(nickname) + "님";
            nicknameSpan.innerText = decodeURIComponent(nickname);
        }
    } else if (userMenu) {
        userMenu.classList.add('d-none');
    }
}

// 2. 실행: 문서가 로드되면 무조건 실행되도록 설정
// 이렇게 하면 이 헤더를 포함하는 모든 페이지에서 자동으로 UI가 갱신.
if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", refreshHeaderUI);
} else {
    refreshHeaderUI();
}

let logoutTimer;
const INACTIVITY_TIME = 20 * 60 * 1000; // 20분으로
// const INACTIVITY_TIME = 20 * 1000; // 20분으로


function resetLogoutTimer() {
    if (logoutTimer) clearTimeout(logoutTimer);
    
    const token = localStorage.getItem('accessToken');
    
    if (token) {
		warningTimer = setTimeout(() => {
            console.log("자동 로그아웃까지 10초 남았습니다.");
        }, INACTIVITY_TIME - (10 * 1000));
		
        logoutTimer = setTimeout(() => {
			console.log("시간이 만료되어 로그아웃을 실행합니다.");
            handleLogout(true);
        }, INACTIVITY_TIME);
    }
}


// 사용자의 움직임을 감지하는 이벤트 리스너들
// 마우스를 움직이거나, 클릭하거나, 키보드를 누르면 타이머를 다시 30분으로 초기화
["mousedown", "mousemove", "keypress", "scroll", "touchstart"].forEach(event => {
    window.addEventListener(event, resetLogoutTimer);
});


// 페이지의 모든 html 요소가 로드 되었을 때 실행 이벤트 리스너
document.addEventListener("DOMContentLoaded", function() {
	
	// URL 쿼리 스트링 분석 객체 생성
    const urlParams = new URLSearchParams(window.location.search);
    
    // URL 파라미터에서 각 인증 정보 추출
	const accessToken = urlParams.get('accessToken');
    const refreshToken = urlParams.get('refreshToken');
    const nickname = urlParams.get('nickname');
    
    // 로그로 확인
    console.log("현재 주소창 파라미터:", window.location.search);
    console.log("추출된 AccessToken:", urlParams.get('accessToken'));
    console.log("추출된 RefreshToken:", urlParams.get('refreshToken'));
    
    
    
    
	// 2. URL에 토큰이 전달되었다면 로컬 스토리지에 저장
    if (accessToken && refreshToken) {
		localStorage.setItem('accessToken', accessToken);
		localStorage.setItem('refreshToken', refreshToken);
		
		// 닉네임(인코딩된 문자열) 변환하여 저장
		if (nickname) {
			localStorage.setItem('nickname', decodeURIComponent(nickname));
		}
		
		console.log("URL로부터 받은 토큰을 로컬 스토리지에 저장 완료!");
		
	}
	
	// '로그인 상태'라면 실행(로그인 상태 공통 처리)
	const savedToken = localStorage.getItem('accessToken');
    if (savedToken) {
        console.log("로그인 상태 확인: 타이머 및 UI 갱신 시작");
        
        // 자동 로그아웃 타이머 초기화 및 시작
        resetLogoutTimer();
        
        // 헤더 UI 로그인 상태에 맞춰서 업데이트
        if (typeof refreshHeaderUI === 'function') refreshHeaderUI();
    }
	
	// 각 HTML 페이지마다 정의된 renderPage 함수 실행(페이지 개별 렌더링)
	if (typeof renderPage === 'function') {
		console.log("해당 페이지의 renderPage를 실행합니다.");
		renderPage();
	}
	
	// 서버에서 여러 메시지를 보낸 경우 처리(에러 처리)
    const error = urlParams.get('error');
    const message = urlParams.get('message');
    
	
	if (error === 'forbidden' && message) {
		alert(decodeURIComponent(message));
		
		// 에러를 알린 후 메인("/")으로 이동
		window.history.replaceState({}, document.title, "/");
	}
	
	
	// 주소창의 토큰를 제거(보안 유지 및 주소창 정리)
	// window.history.replaceState를 사용하면 '새로고침 없이' 주소창 글자만 싹 지워짐
	if (window.location.search.includes('accessToken') || error) {
		// 현재 접속한 페이지 경로는 유지(뒤의 파라미터만 제거)
        window.history.replaceState({}, document.title, window.location.pathname);
        console.log("주소창 파라미터 정리 완료");
    }
	
});




async function testSearch() {
    const token = localStorage.getItem('accessToken'); 

    try {
        const response = await fetch(`http://localhost:9090/api/search/check`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            alert("서버로 신호 보내기 성공! 자바 콘솔을 확인.");
        }else {
            alert("서버 응답 오류: " + response.status);
        }
    } catch (error) {
        alert("신호 보내기 실패: " + error.message);
    }
}



