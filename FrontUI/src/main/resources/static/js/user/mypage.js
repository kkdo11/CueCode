document.addEventListener('DOMContentLoaded', function() {
    // 본인 확인 폼 제출 이벤트
    const verifyForm = document.getElementById('verifyForm');
    verifyForm.onsubmit = async function(e) {
        e.preventDefault();
        // JWT에서 사용자 아이디 추출
        let userId = '';
        const token = document.cookie.split('; ').find(row => row.startsWith('jwtAccessToken='));
        console.log('[로그] 쿠키에서 추출한 jwtAccessToken:', token);
        if (token) {
            try {
                const decoded = jwt_decode(token.split('=')[1]);
                userId = decoded.sub || decoded.userId || '';
                console.log('[로그] 디코딩된 JWT:', decoded);
                console.log('[로그] 추출된 userId:', userId);
            } catch (e) {
                console.error('[로그] JWT 디코딩 에러:', e);
                userId = '';
            }
        } else {
            console.warn('[로그] jwtAccessToken 쿠키 없음');
        }
        const password = document.getElementById('verifyPassword').value;
        const verifyPwMsg = document.getElementById('verifyPwMsg');
        verifyPwMsg.textContent = '';
        console.log('[로그] 입력된 password:', password);
        if (!password) {
            verifyPwMsg.textContent = '비밀번호를 입력하세요.';
            return;
        }
        try {
            console.log('[로그] fetch 요청 시작', {
                url: 'http://localhost:13000/user/verify-password',
                body: { userId, password }
            });
            const res = await fetch('http://localhost:13000/user/verify-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId, password }),
                credentials: 'include'
            });
            console.log('[로그] 서버 응답 status:', res.status);
            let data = {};
            try {
                data = await res.json();
                console.log('[로그] 서버 응답 body:', data);
            } catch (jsonErr) {
                console.error('[로그] 응답 JSON 파싱 에러:', jsonErr);
            }
            if (res.ok && data.result === 1) {
                Swal.fire({
                    icon: 'success',
                    text: '본인 확인이 완료되었습니다.',
                    timer: 1200,
                    showConfirmButton: false
                });
                setTimeout(function() {
                    window.location.href = '/user/myinfo.html';
                }, 1200);
                // TODO: 사용자 정보 수정 폼 활성화 코드 추가
            } else {
                verifyPwMsg.textContent = data.msg || '비밀번호가 올바르지 않습니다.';
                console.warn('[로그] 본인 확인 실패:', data);
            }
        } catch (err) {
            verifyPwMsg.textContent = '서버 오류: 본인 확인 실패';
            console.error('[로그] fetch 에러:', err);
        }
    };
});