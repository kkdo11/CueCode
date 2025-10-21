// 단계 관련 변수 및 함수 제거
let emailVerified = false;
let countdownTimer = null, remaining = 0;
// 인증번호 발송
const emailInput = document.getElementById('email');
const sendCodeBtn = document.getElementById('sendCodeBtn');
const emailCodeInput = document.getElementById('email_code');
const verifyCodeBtn = document.getElementById('verifyCodeBtn');
const emailVerifyMsg = document.getElementById('emailVerifyMsg');

function formatTime(sec) {
    const m = Math.floor(sec / 60).toString().padStart(2, '0');
    const s = (sec % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
}

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
// 인증번호 검증
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
// 비밀번호 유효성 검사
const pwInput = document.getElementById('new_password');
const pw2Input = document.getElementById('new_password2');
const pwValidMsg = document.getElementById('pwValidMsg');
const pwCheckMsg = document.getElementById('pwCheckMsg');

function validatePassword(pw) {
    const lengthValid = pw.length >= 8 && pw.length <= 20;
    const engValid = /[a-zA-Z]/.test(pw);
    const numValid = /[0-9]/.test(pw);
    const specialValid = /[!@#$%^&*(),.?":{}|<>\[\]\\/~`_+=;'\-]/.test(pw);
    return {
        lengthValid,
        engValid,
        numValid,
        specialValid,
        all: lengthValid && engValid && numValid && specialValid
    };
}

pwInput.addEventListener('input', function () {
    const pw = pwInput.value;
    const v = validatePassword(pw);
    if (!pw) {
        pwValidMsg.textContent = '';
        pwValidMsg.className = 'subtext';
        return;
    }
    if (v.all) {
        pwValidMsg.textContent = '사용 가능한 비밀번호입니다.';
        pwValidMsg.className = 'subtext ok';
    } else {
        let msg = '비밀번호는 8~20자, 영문, 숫자, 특수문자를 모두 포함해야 합니다.';
        pwValidMsg.textContent = msg;
        pwValidMsg.className = 'subtext err';
    }
});
pw2Input.addEventListener('input', function () {
    checkPwMatch();
});

function checkPwMatch() {
    const pw = pwInput.value;
    const pw2 = pw2Input.value;
    const v = validatePassword(pw);
    if (!pw2) {
        pwCheckMsg.textContent = '';
        return true;
    }
    if (!v.all) {
        pwCheckMsg.textContent = '비밀번호 조건을 먼저 만족시켜주세요.';
        pwCheckMsg.className = 'subtext err';
        return false;
    }
    if (pw !== pw2) {
        pwCheckMsg.textContent = '비밀번호가 일치하지 않습니다.';
        pwCheckMsg.className = 'subtext err';
        return false;
    } else {
        pwCheckMsg.textContent = '비밀번호가 일치합니다.';
        pwCheckMsg.className = 'subtext ok';
        return true;
    }
}

// 폼 제출
document.getElementById('findPwForm').onsubmit = function (e) {
    e.preventDefault();
    if (!checkPwMatch()) {
        pw2Input.focus();
        return;
    }
    if (!emailVerified) {
        Swal.fire({icon: 'warning', text: '이메일 인증을 완료하세요.'});
        return;
    }
    const user_id = document.getElementById('user_id').value;
    const email = document.getElementById('email').value;
    const new_password = pwInput.value;
    fetch(API_BASE + '/reg/find/resetPassword', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({user_id, email, new_password})
    })
        .then(res => res.json())
        .then(data => {
            if (data.result === 1) {
                Swal.fire({icon: 'success', text: '비밀번호가 성공적으로 변경되었습니다. 로그인 페이지로 이동합니다.'});
                setTimeout(function () {
                    window.location.href = '/user/sign-in.html';
                }, 1200);
            } else {
                Swal.fire({icon: 'error', text: data.msg || '비밀번호 변경 실패'});
            }
        })
        .catch(() => {
            Swal.fire({icon: 'error', text: '비밀번호 변경 요청 실패'});
        });
};