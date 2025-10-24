document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');

    if (loginForm) {
        loginForm.onsubmit = async function(e) {
            e.preventDefault();
            const userId = document.getElementById('userId').value;
            const password = document.getElementById('password').value;

            if (!userId || !password) {
                Swal.fire({
                    icon: 'warning',
                    text: '아이디와 비밀번호를 모두 입력해주세요.'
                });
                return;
            }

            try {
                const res = await fetch(API_BASE + '/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ userId, password }),
                    credentials: 'include'
                });
                const data = await res.json();
                if (res.ok && data.result === 1) { // Removed data.accessToken check
                    // JWT에서 사용자 이름 추출 (서버에서 직접 제공)
                    let userName = data.userName || ''; // Use userName from server response
                    // Removed client-side JWT decoding as accessToken is httpOnly
                    Swal.fire({
                        icon: 'success',
                        html: `${userName ? userName + '님, ' : ''}반가워요!<br>메인 페이지로 이동합니다.`,
                        timer: 1500,
                        showConfirmButton: false
                    });

                    setTimeout(() => {
                        window.location.href = '/index.html';
                    }, 1500);
                } else {
                    Swal.fire({
                        icon: 'error',
                        text: data.msg || '로그인 실패',
                        showConfirmButton: true
                    });
                }
            } catch (err) {
                Swal.fire({
                    icon: 'error',
                    text: '로그인 요청 실패'
                });
                console.error('로그인 요청 중 오류 발생:', err);
            }
        };
    } else {
        console.error("오류: 'loginForm' 요소를 찾을 수 없습니다.");
    }
});