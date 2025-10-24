document.addEventListener('DOMContentLoaded', function() {

    async function getUserId() {
        try {
            const response = await fetch(API_BASE + '/user/me', {
                method: 'GET',
                credentials: 'include'
            });

            if (response.ok) {
                const data = await response.json();
                return data.userId || '';
            } else {
                console.error('Failed to fetch user ID:', response.status);
                return '';
            }
        } catch (error) {
            console.error('Error fetching user ID:', error);
            return '';
        }
    }

    const verifyForm = document.getElementById('verifyForm');
    verifyForm.onsubmit = async function(e) {
        e.preventDefault();

        const userId = await getUserId();

        if (!userId) {
            Swal.fire({
                icon: 'error',
                text: '사용자 정보를 가져올 수 없습니다. 다시 로그인해주세요.',
            });
            return;
        }

        const password = document.getElementById('verifyPassword').value;
        const verifyPwMsg = document.getElementById('verifyPwMsg');
        verifyPwMsg.textContent = '';

        if (!password) {
            verifyPwMsg.textContent = '비밀번호를 입력하세요.';
            return;
        }

        try {
            console.log('[로그] fetch 요청 시작', {
                url: API_BASE + '/user/verify-password',
                body: { userId, password }
            });
            const res = await fetch(API_BASE + '/user/verify-password', {
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
                    window.location.href = '/user/myInfo.html';
                }, 1200);
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