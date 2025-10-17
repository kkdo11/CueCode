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
                const res = await fetch('http://localhost:13000/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ userId, password }),
                    credentials: 'include'
                });
                const data = await res.json();
                if (res.ok && data.result === 1 && data.accessToken) {
                    document.cookie = `jwtAccessToken=${data.accessToken}; path=/; SameSite=Lax; max-age=3600;`;
                    console.log('토큰을 쿠키에 저장 완료');
                    // JWT에서 사용자 이름 추출
                    let userName = '';
                    try {
                        const decoded = jwt_decode(data.accessToken);
                        userName = decoded.userName || '';
                    } catch (e) {
                        userName = '';
                    }
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