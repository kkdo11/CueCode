document.getElementById('contact-form').addEventListener('submit', function(event) {
    event.preventDefault();

    const name = document.getElementById('formGroupExampleInput').value;
    const email = document.getElementById('exampleInputEmail1').value;
    const message = document.getElementById('exampleFormControlTextarea1').value;

    if (!name || !email || !message) {
        Swal.fire({
            icon: 'warning',
            title: '입력 필요',
            text: '모든 필드를 입력해주세요.',
        });
        return;
    }

    const formData = { name, email, message };

    fetch(API_BASE + '/users/contact', {
        method: 'POST',
        credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + getCookie('jwtAccessToken')
            },
            body: JSON.stringify(formData)
    })
        .then(response => {
            if (response.ok) {
                return response.text().then(text => {
                    try { return JSON.parse(text); } catch (e) { return text; }
                });
            } else if (response.status === 401) {
                throw new Error('로그인 후 이용해주세요.');
            } else {
                throw new Error(`메시지 전송에 실패했습니다. (상태 코드: ${response.status})`);
            }
        })
        .then(data => {
            Swal.fire({
                icon: 'success',
                title: '전송 완료',
                text: '소중한 의견 감사합니다.',
            });
            document.getElementById('contact-form').reset();
        })
        .catch((error) => {
            console.error('Error:', error);
            Swal.fire({
                icon: 'error',
                title: '오류',
                text: error.message,
            });
        });
});
// --- [1] 유틸리티 함수 정의 ---
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

function removeCookie(name) {
    document.cookie = `${name}=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT;`;
}

// --- [2] 로그아웃 버튼 노출 함수 정의 ---
function showLogoutButton(userName) {
    const authBtnGroup = document.getElementById('auth-btn-group');
    if (authBtnGroup) {
        // 이 부분을 dark 텍스트로 수정하여 헤더 토글 메뉴에서도 잘 보이게 조정합니다.
        authBtnGroup.innerHTML = `<span class="me-3 fw-bold text-dark">${userName ? userName + '님' : ''} 환영합니다!</span>
            <button id="logoutBtn" class="btn btn-outline-danger px-4 py-2">로그아웃</button>`; // ✅ px-3 -> px-4 수정
        document.getElementById('logoutBtn').onclick = function() {
            // 서버 로그아웃 API 호출
            fetch(API_BASE + '/user/v1/logout', { method: 'POST', credentials: 'include' })
                .finally(() => {
                    removeCookie('jwtAccessToken');
                    removeCookie('jwtRefreshToken');
                    window.location.href = 'index.html';
                });
        };
        console.log('로그아웃 버튼 노출 완료');
    }
}

// --- [3] DOMContentLoaded 메인 로직 (배너 버튼 및 헤더 버튼 변경) ---
window.addEventListener('DOMContentLoaded', function() {
    const token = getCookie('jwtAccessToken');
    const dashboardLink = document.getElementById('dashboardLink');
    const dashboardText = document.getElementById('dashboardText');

    // 헤더 메뉴 링크 요소
    // ID 기반으로 다시 찾도록 수정: HTML에 ID를 추가했기 때문에 정확한 접근이 가능합니다.
    const homeMenuItem = document.getElementById('homeMenuItem');
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
        console.log('미로그인 상태: 서비스 시작 버튼 설정');
        if (dashboardText) dashboardText.textContent = " "; // 텍스트를 " "에서 "서비스 시작"으로 복구
        if (dashboardLink) dashboardLink.href = 'sign-in.html';

        // 미로그인 상태에서는 대시보드와 마이페이지 메뉴를 숨김 (<li> 요소 숨김)
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

        // ✅ 역할 클레임 병합: 'roles' 키의 값을 'role' 키로 사용
        decoded.role = decoded.role || decoded.roles;

    } catch (e) {
        console.error('토큰 디코딩 실패. 토큰 만료 또는 오류:', e);
        // 토큰 문제 발생 시 쿠키를 지우고 미로그인 상태로 복귀
        removeCookie('jwtAccessToken');
        window.location.reload();
        return;
    }

    // 3. 역할(ROLE)에 따른 배너/헤더 UI 변경
    if (decoded && decoded.role) { // 👈 이제 decoded.role을 사용하여 정상 분기
        let linkHref = 'index.html';
        let linkText = `${decoded.userName}님 대시보드`;

        // 모든 메뉴를 일단 숨기고, 역할에 맞는 메뉴만 표시 (<li> 요소 초기화)
        if (patientMenuItem) patientMenuItem.style.display = 'none';
        if (managerMenuItem) managerMenuItem.style.display = 'none';
        if (myPageMenuItem) myPageMenuItem.style.display = 'none';

        // 마이페이지는 로그인 시 항상 보이도록 설정
        if (myPageMenuItem && myPageMenuLinkAnchor) {
            myPageMenuLinkAnchor.href = '../user/mypage.html';
            myPageMenuItem.style.display = 'list-item'; // <li> 표시
        }

        // 환자 및 관리자 메뉴 링크 초기화 (나중에 링크를 사용할 수 있도록)
        if (patientMenuLink) patientMenuLink.href = '../patient/dashboard.html';
        if (managerMenuLink) managerMenuLink.href = '../manager/dashboard.html';


        if (decoded.role === 'ROLE_USER') {
            // 환자 역할
            linkHref = '../patient/dashboard.html';
            linkText = '환자 대시보드';
            if (patientMenuItem) {
                patientMenuItem.style.display = 'list-item'; // <li> 표시
            }
        } else if (decoded.role === 'ROLE_USER_MANAGER') {
            // 관리자(보호자) 역할
            linkHref = '../manager/dashboard.html'; // ✅ 수정된 올바른 상대 경로
            linkText = '관리자 대시보드';
            if (managerMenuItem) {
                managerMenuItem.style.display = 'list-item'; // <li> 표시
            }
        }

        // 배너 버튼 UI 업데이트
        if (dashboardLink) dashboardLink.href = linkHref;
        if (dashboardText) dashboardText.textContent = linkText;

        // 헤더의 로그인/로그아웃 버튼 업데이트
        showLogoutButton(decoded.userName);


    } else {
        console.log('로그인 상태지만 역할(role) 정보가 불분명함. 로그아웃 버튼만 노출.');
        if (dashboardText) dashboardText.textContent = " ";

        // 역할 정보가 없어도 로그인 상태이므로 로그아웃 버튼은 보여줍니다.
        if (decoded && decoded.userName) {
            showLogoutButton(decoded.userName);
            // 역할이 불분명해도 마이페이지는 보이게 설정 (선택 사항)
            if (myPageMenuItem) myPageMenuItem.style.display = 'list-item';
        }
    }
});