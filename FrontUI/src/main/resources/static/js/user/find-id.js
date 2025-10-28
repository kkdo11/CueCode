// emailVerified, countdownTimer, remaining 변수는 find-password.js와 동일하게 유지
let emailVerified = false;
let countdownTimer = null, remaining = 0;

// DOM 요소 가져오기
const emailInput = document.getElementById('email');
const sendCodeBtn = document.getElementById('sendCodeBtn');
const emailCodeInput = document.getElementById('email_code');
const verifyCodeBtn = document.getElementById('verifyCodeBtn');
const emailVerifyMsg = document.getElementById('emailVerifyMsg');
const foundIdDisplay = document.getElementById('foundIdDisplay');
const foundUserIdInput = document.getElementById('found_user_id');
const findIdForm = document.getElementById('findIdForm');

// 시간 포맷 함수 (재사용)
function formatTime(sec) {
    const m = Math.floor(sec / 60).toString().padStart(2, '0');
    const s = (sec % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
}

// 카운트다운 시작 함수 (재사용)
function startCountdown(seconds) {
    remaining = seconds;
    document.getElementById('countdown').textContent = formatTime(remaining);
    clearInterval(countdownTimer);
    countdownTimer = setInterval(() => {
        remaining--;
        document.getElementById('countdown').textContent = formatTime(Math.max(remaining, 0));
        if (remaining <= 0) {
            clearInterval(countdownTimer);
            emailVerifyMsg.textContent = '인증 시간이 만료되었습니다.';
            verifyCodeBtn.disabled = true;
            sendCodeBtn.disabled = false;
            sendCodeBtn.textContent = '인증번호 재발송';
        }
    }, 1000);
}

// 인증번호 발송 버튼 클릭 이벤트 (재사용)
sendCodeBtn.onclick = function () {
    const email = emailInput.value.trim();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        Swal.fire({icon: 'warning', text: '올바른 이메일 주소를 입력하세요.'});
        return;
    }
    fetch(API_BASE + '/reg/sendMail', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email: email})
    })
        .then(res => res.json())
        .then(data => {
            if (data.result === 1) {
                startCountdown(180);
                emailCodeInput.value = '';
                emailCodeInput.focus();
                sendCodeBtn.textContent = '발송 완료';
                verifyCodeBtn.disabled = false;
                Swal.fire({icon: 'success', text: '인증메일이 발송되었습니다. 메일을 확인하세요.'});
            } else {
                Swal.fire({icon: 'error', text: '메일 발송 실패: ' + (data.msg || '서버 오류')});
            }
        })
        .catch(() => {
            Swal.fire({icon: 'error', text: '메일 발송 요청 실패'});
        });
};

// 인증번호 검증 버튼 클릭 이벤트 (재사용)
verifyCodeBtn.onclick = function () {
    const email = emailInput.value.trim();
    const authCode = emailCodeInput.value.trim();
    fetch(API_BASE + '/reg/verifyEmailAuth', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email: email, authCode: authCode})
    })
        .then(res => res.json())
        .then(data => {
            if (data.result === 1) {
                clearInterval(countdownTimer);
                emailVerified = true;
                emailVerifyMsg.textContent = '이메일 인증이 완료되었습니다.';
                verifyCodeBtn.disabled = true;
                sendCodeBtn.disabled = true;
                sendCodeBtn.textContent = '인증 완료';
                Swal.fire({icon: 'success', text: '이메일 인증이 완료되었습니다.'});
            } else {
                emailVerified = false;
                emailVerifyMsg.textContent = data.msg || '인증번호가 일치하지 않습니다.';
                Swal.fire({icon: 'error', text: data.msg || '인증번호가 일치하지 않습니다.'});
            }
        })
        .catch((err) => {
            console.error('이메일 인증 요청 오류:', err);
            emailVerified = false;
            emailVerifyMsg.textContent = '서버 인증 요청 실패. 다시 시도해주세요.';
            Swal.fire({icon: 'error', text: '서버 인증 요청 실패. 다시 시도해주세요.'});
        });
};

// 폼 제출 이벤트 (아이디 찾기 로직)
findIdForm.onsubmit = function (e) {
    e.preventDefault(); // 기본 폼 제출 방지

    if (!emailVerified) {
        Swal.fire({icon: 'warning', text: '이메일 인증을 완료하세요.'});
        return;
    }

    const email = emailInput.value.trim();

    fetch(API_BASE + '/reg/findIdByEmail', { // 새로 추가한 백엔드 API 호출
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email: email})
    })
        .then(res => res.json())
        .then(data => {
            if (data.result === 1 && data.users && data.users.length > 0) {
                // 아이디 목록을 표시할 컨테이너 초기화
                foundIdDisplay.innerHTML = ''; // 기존 내용 지우기

                const ul = document.createElement('ul');
                ul.style.listStyleType = 'none';
                ul.style.padding = '0';

                data.users.forEach(user => {
                    const li = document.createElement('li');
                    li.style.marginBottom = '5px';
                    li.style.fontSize = '1.1em';
                    li.innerHTML = `<strong>${user.userId}</strong> (${user.userType === 'patient' ? '환자' : '관리자'})`;
                    ul.appendChild(li);
                });

                foundIdDisplay.appendChild(ul);
                foundIdDisplay.style.display = 'block'; // 아이디 표시 영역 보이게 함
                Swal.fire({icon: 'success', title: '아이디를 찾았습니다!', html: '등록된 아이디 목록입니다.'});
            } else {
                foundIdDisplay.style.display = 'none'; // 아이디 표시 영역 숨김
                Swal.fire({icon: 'error', text: data.msg || '일치하는 아이디를 찾을 수 없습니다.'});
            }
        })
        .catch(() => {
            Swal.fire({icon: 'error', text: '아이디 찾기 요청 실패'});
        });
};
