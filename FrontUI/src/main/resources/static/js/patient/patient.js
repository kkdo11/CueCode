// --- [1] 유틸리티 함수 정의 ---
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

function removeCookie(name) {
    document.cookie = `${name}=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT;`;
    document.cookie = `${name}=; path=/; domain=localhost; expires=Thu, 01 Jan 1970 00:00:00 GMT;`;
}

// --- [2] 로그아웃 버튼 노출 함수 정의 ---
function showLogoutButton(userName) {
    const authBtnGroup = document.getElementById('auth-btn-group');
    if (authBtnGroup) {
        authBtnGroup.innerHTML = `<span class="me-3 fw-bold text-dark">${userName ? userName + '님' : ''} 환영합니다!</span>
                <button id="logoutBtn" class="btn btn-outline-danger px-4 py-2">로그아웃</button>`;
        document.getElementById('logoutBtn').onclick = function() {
            // 서버 로그아웃 API 호출
            fetch('http://localhost:13000/user/v1/logout', { method: 'POST', credentials: 'include' })
                .finally(() => {
                    removeCookie('jwtAccessToken');
                    removeCookie('jwtRefreshToken');
                    window.location.href = 'sign-in.html';
                });
        };
        console.log('로그아웃 버튼 노출 완료');
    }
}

// --- [3] DOMContentLoaded 메인 로직 (헤더 버튼 변경) ---
window.addEventListener('DOMContentLoaded', function() {
    const token = getCookie('jwtAccessToken');

    // 헤더 메뉴 링크 요소
    const patientMenuItem = document.getElementById('patientMenuItem');
    const managerMenuItem = document.getElementById('managerMenuItem');
    const myPageMenuItem = document.getElementById('myPageMenuItem');

    // <a> 태그 자체 (링크 변경을 위해 필요)
    const patientMenuLink = document.getElementById('patientMenuLink');
    const managerMenuLink = document.getElementById('managerMenuLink');
    const myPageMenuLinkAnchor = document.getElementById('myPageMenuLinkAnchor');

    console.log('쿠키에서 jwtAccessToken:', token ? '토큰 존재' : '토큰 없음');

    // 1. 미로그인 상태 처리
    if (!token) {
        console.log('미로그인 상태: 대시보드 및 마이페이지 메뉴 숨김');
        if (patientMenuItem) patientMenuItem.style.display = 'none';
        if (managerMenuItem) managerMenuItem.style.display = 'none';
        if (myPageMenuItem) myPageMenuItem.style.display = 'none';
        return;
    }

    // 2. 로그인 상태 처리 (토큰 존재)
    let decoded = null;
    try {
        decoded = jwt_decode(token);
        console.log('토큰 디코딩 결과:', decoded);

        // 역할 클레임 병합: 'roles' 키의 값을 'role' 키로 사용
        decoded.role = decoded.role || decoded.roles;

    } catch (e) {
        console.error('토큰 디코딩 실패. 토큰 만료 또는 오류:', e);
        removeCookie('jwtAccessToken');
        window.location.reload();
        return;
    }

    // 3. 역할(ROLE)에 따른 헤더 UI 변경
    if (decoded && decoded.role) {

        // 모든 메뉴를 일단 숨기고, 역할에 맞는 메뉴만 표시
        if (patientMenuItem) patientMenuItem.style.display = 'none';
        if (managerMenuItem) managerMenuItem.style.display = 'none';
        if (myPageMenuItem) myPageMenuItem.style.display = 'none';

        // 마이페이지는 로그인 시 항상 보이도록 설정
        if (myPageMenuItem && myPageMenuLinkAnchor) {
            myPageMenuLinkAnchor.href = '../user/mypage.html';
            myPageMenuItem.style.display = 'list-item'; // <li> 표시
        }

        if (decoded.role === 'ROLE_USER' && patientMenuItem) {
            // 환자 역할
            patientMenuItem.style.display = 'list-item'; // <li> 표시
        } else if (decoded.role === 'ROLE_USER_MANAGER' && managerMenuItem) {
            // 관리자(보호자) 역할
            managerMenuItem.style.display = 'list-item'; // <li> 표시
        }

        // 헤더의 로그인/로그아웃 버튼 업데이트
        showLogoutButton(decoded.userName);


    } else {
        console.log('로그인 상태지만 역할(role) 정보가 불분명함. 로그아웃 버튼과 마이페이지 버튼만 노출.');

        // 역할 정보가 없어도 로그인 상태이므로 로그아웃 버튼은 보여줍니다.
        if (decoded && decoded.userName) {
            showLogoutButton(decoded.userName);
            // 마이페이지는 보이게 설정
            if (myPageMenuItem) myPageMenuItem.style.display = 'list-item';
        }
    }
});