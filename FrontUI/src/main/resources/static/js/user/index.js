// 전역 변수: API_BASE는 외부에서 정의되었다고 가정
// const API_BASE = '...';

/**
 * 폼 제출 이벤트 리스너 설정
 */
document.getElementById('contact-form')?.addEventListener('submit', function(event) {
    event.preventDefault();

    // 1. 입력 값 가져오기
    const nameInput = document.getElementById('formGroupExampleInput');
    const emailInput = document.getElementById('exampleInputEmail1');
    const messageInput = document.getElementById('exampleFormControlTextarea1');

    const name = nameInput?.value;
    const email = emailInput?.value;
    const message = messageInput?.value;

    // 2. 입력 값 유효성 검사
    if (!name || !email || !message) {
        Swal.fire({
            icon: 'warning',
            title: '입력 필요',
            text: '모든 필드를 입력해주세요.',
        });
        return;
    }

    const formData = { name, email, message };

    // 3. API 호출
    fetch(API_BASE + '/users/contact', {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            // 'Authorization': 'Bearer ' + getCookie('jwtAccessToken') // httpOnly 쿠키는 자동 전송되므로 주석 처리 유지
        },
        body: JSON.stringify(formData)
    })
        .then(response => {
            if (response.ok) {
                // 성공 응답 처리 (응답 본문이 JSON일 수도, 아닐 수도 있음)
                return response.text().then(text => {
                    try { return JSON.parse(text); } catch (e) { return text; }
                });
            }

            // 에러 응답 처리
            if (response.status === 401) {
                throw new Error('로그인 후 이용해주세요.');
            }

            throw new Error(`메시지 전송에 실패했습니다. (상태 코드: ${response.status})`);
        })
        .then(data => {
            // 성공 후 처리
            Swal.fire({
                icon: 'success',
                title: '전송 완료',
                text: '소중한 의견 감사합니다.',
            });
            document.getElementById('contact-form').reset();
        })
        .catch((error) => {
            // 오류 처리
            console.error('문의 폼 전송 오류:', error);
            Swal.fire({
                icon: 'error',
                title: '오류',
                text: error.message,
            });
        });
});


// ----------------------------------------------------------------------
// --- [1] 인증 및 UI 렌더링 관련 유틸리티 함수 ---
// ----------------------------------------------------------------------

/**
 * 사용자 이름에 따라 인증 버튼 그룹을 렌더링합니다.
 * @param {string | null} userName 사용자 이름 (로그인 상태가 아닐 경우 null)
 */
function renderAuthButtonGroup(userName) {
    // 1. 요소 찾기
    const authBtnGroup = document.getElementById('auth-btn-group');
    if (!authBtnGroup) {
        console.error("Error: 'auth-btn-group' 요소를 찾을 수 없습니다.");
        return;
    }

    // 2. 로그인 상태별 렌더링
    if (userName) {
        // 로그인 상태: 환영 메시지 및 로그아웃 버튼 노출
        authBtnGroup.innerHTML = `
            <span class="me-3 fw-bold text-dark">${userName}님 환영합니다!</span>
            <button id="logoutBtn" class="btn btn-outline-danger px-4 py-2">로그아웃</button>
        `;

        // 로그아웃 버튼 이벤트 리스너 설정
        document.getElementById('logoutBtn')?.addEventListener('click', function() {
            // 서버 로그아웃 API 호출 (httpOnly 쿠키 삭제를 서버에서 처리)
                                    fetch(API_BASE + '/login/logout', { method: 'POST', credentials: 'include' })
                .finally(() => {
                    // 클라이언트 쿠키 제거 및 페이지 리로드 (JWT 디코딩 방식의 잔재로 보이며,
                    // 서버가 httpOnly 쿠키를 사용하는 경우 클라이언트 측 삭제는 불필요하지만,
                    // 기존 로직 유지를 위해 남겨둡니다.)
                    if (typeof removeCookie === 'function') {
                        removeCookie('jwtAccessToken');
                        removeCookie('jwtRefreshToken');
                    }
                    window.location.href = 'index.html';
                });
        });
        console.log('헤더: 로그아웃 버튼 렌더링 완료');
    } else {
        // 미로그인 상태: 로그인 및 회원가입 버튼 노출 (Bootstrap Flex 유틸리티 활용)
        // hstack과 w-50을 사용하여 두 버튼이 50%씩 너비를 차지하며 수평 정렬되도록 구성
        authBtnGroup.innerHTML = `
            <a href="/user/sign-in.html" class="btn btn-outline-light fs-6 bg-white px-3 py-2 text-dark w-50 hstack justify-content-center">로그인</a>
            <a href="/user/sign-up.html" class="btn btn-dark text-white fs-6 bg-dark px-3 py-2 w-50 hstack justify-content-center">회원가입</a>
        `;
        console.log('헤더: 로그인/회원가입 버튼 렌더링 완료');
    }
}


/**
 * 로그인 상태 확인 API를 호출하여 인증 데이터를 가져옵니다. (SRP 적용)
 * @returns {Promise<{isLoggedIn: boolean, userName: string, userRole: string}>} 인증 데이터 객체
 */
async function getAuthData() {
    try {
                const response = await fetch(API_BASE + '/user/me', {
            method: 'GET',
            credentials: 'include' // httpOnly 쿠키 자동 전송
        });

        if (response.ok) {
            const data = await response.json();
            if (data.userId) {
                console.log(`로그인 성공: userId=${data.userId}, userName=${data.userName}, userRole=${data.userRole}`);

                // 전역 변수에 userRole 저장 (다른 스크립트에서 사용 가능)
                window.userRole = data.userRole || '';
                window.userId   = data.userId;                 // ✅ 전역 userId
                window.userName = data.userName || '사용자';   // (원하면 같이 저장)

                return {
                    isLoggedIn: true,
                    userName: data.userName || '사용자',
                    userRole: data.userRole || ''
                };
            }
        }

        // 401 Unauthorized 또는 userId가 없는 성공 응답
        console.log(`미로그인 상태: /user/me 응답 상태=${response.status}`);

    } catch (e) {
        console.error('로그인 상태 확인 API 호출 실패:', e);
    }

    // 기본 미로그인 상태 반환 (userRole도 초기화)
    window.userRole = '';
    return { isLoggedIn: false, userName: '', userRole: '' };
}

/**
 * 필요한 모든 UI 요소를 가져옵니다. (SRP 적용)
 * @returns {object} UI 요소 맵
 */
function getUIElements() {
    return {
        dashboardLink: document.getElementById('dashboardLink'),
        dashboardText: document.getElementById('dashboardText'),
        patientMenuItem: document.getElementById('patientMenuItem'),
        managerMenuItem: document.getElementById('managerMenuItem'),
        myPageMenuItem: document.getElementById('myPageMenuItem'),
        patientMenuLink: document.getElementById('patientMenuLink'),
        managerMenuLink: document.getElementById('managerMenuLink'),
        myPageMenuLinkAnchor: document.getElementById('myPageMenuLinkAnchor'),
    };
}

/**
 * 인증 데이터에 따라 UI를 렌더링합니다. (SRP 적용)
 * @param {{isLoggedIn: boolean, userName: string, userRole: string}} authData 인증 데이터
 */
function renderUI(authData) {
    const elements = getUIElements();
    const { isLoggedIn, userName, userRole } = authData;

    // 모든 메뉴를 기본적으로 숨기거나 적절히 설정합니다.
    elements.patientMenuItem && (elements.patientMenuItem.style.display = 'none');
    elements.managerMenuItem && (elements.managerMenuItem.style.display = 'none');
    elements.myPageMenuItem && (elements.myPageMenuItem.style.display = 'none');

    // 로그인/로그아웃 버튼 그룹 렌더링
    renderAuthButtonGroup(isLoggedIn ? userName : null);

    // 1. 미로그인 상태 처리
    if (!isLoggedIn) {
        console.log('UI 렌더링: 미로그인 모드');
        if (elements.dashboardText) elements.dashboardText.textContent = "서비스 시작";
        if (elements.dashboardLink) elements.dashboardLink.href = '/user/sign-in.html';
        return;
    }

    // 2. 로그인 상태 처리
    console.log('UI 렌더링: 로그인 모드 (역할별 메뉴 설정)');

    let linkHref = 'index.html'; // 기본값
    let linkText = `${userName}님 대시보드`; // 기본값

    // 마이페이지는 로그인 시 항상 보이도록 설정
    if (elements.myPageMenuItem && elements.myPageMenuLinkAnchor) {
        elements.myPageMenuLinkAnchor.href = '/user/mypage.html';
        elements.myPageMenuItem.style.display = 'list-item';
    }

    // 환자 및 관리자 메뉴 링크 초기화 (실제 대시보드 URL 설정)
    if (elements.patientMenuLink) elements.patientMenuLink.href = '/patient/dashboard.html';
    if (elements.managerMenuLink) elements.managerMenuLink.href = '/manager/dashboard.html';

    // 역할에 따른 대시보드 링크 및 메뉴 노출 설정
    if (userRole === 'ROLE_USER') {
        // 환자 역할
        linkHref = '/patient/dashboard.html';
        linkText = '환자 대시보드';
        if (elements.patientMenuItem) {
            elements.patientMenuItem.style.display = 'list-item';
        }
    } else if (userRole === 'ROLE_USER_MANAGER') {
        // 관리자(보호자) 역할
        linkHref = '/manager/dashboard.html';
        linkText = '관리자 대시보드';
        if (elements.managerMenuItem) {
            elements.managerMenuItem.style.display = 'list-item';
        }
    }

    // 배너 버튼 UI 업데이트
    if (elements.dashboardLink) elements.dashboardLink.href = linkHref;
    if (elements.dashboardText) elements.dashboardText.textContent = linkText;
}


// ----------------------------------------------------------------------
// --- [2] 메인 로직 실행 함수 ---
// ----------------------------------------------------------------------

/**
 * 로그인 상태를 확인하고 그 결과에 따라 UI를 렌더링하는 메인 함수
 */
async function checkLoginStatusAndRenderUI() {
    // 1. 인증 데이터 가져오기
    const authData = await getAuthData();

    // 2. 인증 데이터에 따라 UI 렌더링
    renderUI(authData);
}


// ----------------------------------------------------------------------
// --- [3] DOMContentLoaded 이벤트 리스너 ---
// ----------------------------------------------------------------------

window.addEventListener('DOMContentLoaded', function() {
    checkLoginStatusAndRenderUI();
});