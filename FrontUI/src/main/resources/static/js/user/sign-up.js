// 단계 관리
const steps = ["step1", "step2", "step3", "step4"];
let currentStep = 0;
let role = "patient";
let detectionAreaType = "hand";

function showStep(idx) {
    steps.forEach((id, i) => {
        document.getElementById(id).style.display = i === idx ? "block" : "none";
        document.getElementById(id).classList.toggle("active", i === idx);
        document.getElementById(`step${i+1}-bar`).classList.toggle("active", i === idx);
    });
    // 감지방식 단계(4)는 보호자면 숨김
    document.getElementById("step4-bar").style.display = (role === "patient") ? "block" : "none";
    document.getElementById("step4").style.display = (role === "patient" && currentStep === 3) ? "block" : "none";

    // 보호자 회원가입 시 3단계에서 약관 동의와 회원가입 버튼 노출
    const managerTerms = document.getElementById('manager-terms');
    const managerSignupBtn = document.getElementById('manager-signup-btn');
    const managerPrevBtn = document.getElementById('manager-prev-btn');

    if (currentStep === 2 && role === 'manager') {
        managerTerms.style.display = 'block';
        managerSignupBtn.style.display = 'inline-block';
        // 왼쪽 화살표(이전 버튼) 숨김
        managerPrevBtn.style.display = 'none';
    } else {
        managerTerms.style.display = 'none';
        managerSignupBtn.style.display = 'none';
    }

    // 보호자 이메일 인증 단계에서는 오른쪽 화살표(next-btn) 숨김
    const step3NextBtn = document.querySelector('#step3-btns .next-btn');
    if (currentStep === 2 && role === 'manager') {
        if (step3NextBtn) step3NextBtn.style.display = 'none';
    } else {
        if (step3NextBtn) step3NextBtn.style.display = '';
    }

    updateProgressBar(idx + 1);
}
function nextStep() {
    if (currentStep === 0) { // 역할 선택
        showStep(++currentStep);
    } else if (currentStep === 1) { // 기본정보
        if (!validateStep2()) return;
        showStep(++currentStep);
    } else if (currentStep === 2) { // 이메일
        if (!validateStep3()) return;
        if (role === "manager") { submitForm(); return; }
        showStep(++currentStep);
    } else if (currentStep === 3) { // 감지방식
        submitForm();
    }
}
function prevStep() {
    if (currentStep > 0) {
        currentStep--;
        showStep(currentStep);
    }
}
// 역할 버튼
const patientBtn = document.getElementById('patientBtn');
const managerBtn = document.getElementById('managerBtn');
patientBtn.onclick = function () { role = 'patient'; this.classList.add('active'); managerBtn.classList.remove('active'); };
managerBtn.onclick = function () { role = 'manager'; this.classList.add('active'); patientBtn.classList.remove('active'); };
// 감지방식 버튼
Array.from(document.querySelectorAll('.detect-btn')).forEach(btn => {
    btn.onclick = function () {
        Array.from(document.querySelectorAll('.detect-btn')).forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        detectionAreaType = this.dataset.value;
    };
});
// 다음/이전 버튼 이벤트
Array.from(document.querySelectorAll('.next-btn')).forEach(btn => btn.onclick = nextStep);
Array.from(document.querySelectorAll('.prev-btn')).forEach(btn => btn.onclick = prevStep);
// 비밀번호 확인
const pwInput = document.getElementById('password');
const pw2Input = document.getElementById('password2');
const pwValidMsg = document.getElementById('pwValidMsg');
const pwCheckMsg = document.getElementById('pwCheckMsg');
// 비밀번호 유효성 검사 함수
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
// 비밀번호 입력 시 실시간 유효성 안내
pwInput.addEventListener('input', function() {
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
// 비밀번호 확인 입력 시에도 유효성 체크
pw2Input.addEventListener('input', function() {
    checkPwMatch();
});
// 비밀번호 일치 및 유효성 통합 체크
function checkPwMatch() {
    const pw = pwInput.value;
    const pw2 = pw2Input.value;
    const v = validatePassword(pw);
    if (!pw2) { pwCheckMsg.textContent = ''; return true; }
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
function validateStep2() {
    if (!document.getElementById('user_id').value.trim()) {
        Swal.fire({
            icon: 'warning',
            text: '아이디를 입력하세요.'
        });
        return false;
    }
    if (!document.getElementById('user_name').value.trim()) {
        Swal.fire({
            icon: 'warning',
            text: '이름을 입력하세요.'
        });
        return false;
    }
    if (!pwInput.value) {
        Swal.fire({
            icon: 'warning',
            text: '비밀번호를 입력하세요.'
        });
        return false;
    }
    if (!pw2Input.value) {
        Swal.fire({
            icon: 'warning',
            text: '비밀번호를 재입력하세요.'
        });
        return false;
    }
    if (!checkPwMatch()) { pw2Input.focus(); return false; }
    return true;
}
// 이메일 인증(간략화)
const emailInput = document.getElementById('email');
const sendCodeBtn = document.getElementById('sendCodeBtn');
const emailVerifyGroup = document.getElementById('emailVerifyGroup');
const emailCodeInput = document.getElementById('email_code');
const verifyCodeBtn = document.getElementById('verifyCodeBtn');
const emailVerifyMsg = document.getElementById('emailVerifyMsg');
let sentCode = null, emailVerified = false, countdownTimer = null, remaining = 0;
function formatTime(sec){ const m = Math.floor(sec/60).toString().padStart(2,'0'); const s = (sec%60).toString().padStart(2,'0'); return `${m}:${s}`; }
function startCountdown(seconds){ remaining = seconds; document.getElementById('countdown').textContent = formatTime(remaining); clearInterval(countdownTimer); countdownTimer = setInterval(()=>{
 remaining--;
 document.getElementById('countdown').textContent = formatTime(Math.max(remaining,0));
 if(remaining<=0){
 clearInterval(countdownTimer);
 emailVerifyMsg.textContent = '인증 시간이 만료되었습니다.';
 verifyCodeBtn.disabled = true;
 sendCodeBtn.disabled = false;
 sendCodeBtn.textContent = '인증번호 재발송';
 }
},1000); }
sendCodeBtn.onclick = function() {
    const email = emailInput.value.trim();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        Swal.fire({
            icon: 'warning',
            text: '올바른 이메일 주소를 입력하세요.'
        });
        return;
    }
    // 서버로 이메일만 전송, 인증번호 및 HTML 메일 발송은 백엔드에서 처리
    fetch(API_BASE + '/reg/sendMail', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email })
    })
        .then(res => res.json())
        .then(data => {
            if (data.result === 1) {
                emailVerifyGroup.style.display = 'block';
                startCountdown(180);
                emailCodeInput.value = '';
                emailCodeInput.focus();
                sendCodeBtn.textContent = '발송 완료';
                Swal.fire({
                    icon: 'success',
                    text: '인증메일이 발송되었습니다. 메일을 확인하세요.'
                });
            } else {
                Swal.fire({
                    icon: 'error',
                    text: '메일 발송 실패: ' + data.msg
                });
            }
        })
        .catch(() => {
            Swal.fire({
                icon: 'error',
                text: '메일 발송 요청 실패'
            });
        });
};

// 중복된 이메일 인증 함수 제거하고 하나로 통합
verifyCodeBtn.onclick = function() {
    const email = emailInput.value.trim();
    const authCode = emailCodeInput.value.trim();
    // 서버 인증 요청
    fetch(API_BASE + '/reg/verifyEmailAuth', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email, authCode: authCode })
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
            } else {
                emailVerified = false;
                emailVerifyMsg.textContent = data.msg || '인증번호가 일치하지 않습니다.';
            }
        })
        .catch((err) => {
            console.error('이메일 인증 요청 오류:', err);
            emailVerified = false;
            emailVerifyMsg.textContent = '서버 인증 요청 실패. 다시 시도해주세요.';
        });
};
function validateStep3() {
    if (!emailInput.value.trim()) {
        Swal.fire({
            icon: 'warning',
            text: '이메일을 입력하세요.'
        });
        return false;
    }
    if (!emailVerified) {
        Swal.fire({
            icon: 'warning',
            text: '이메일 인증을 완료하세요.'
        });
        return false;
    }
    return true;
}
// 폼 제출
function submitForm() {
    const user_id = document.getElementById('user_id').value;
    const user_name = document.getElementById('user_name').value;
    const password = document.getElementById('password').value;
    const email = document.getElementById('email').value;
    let url = '';
    if (role === 'patient') {
        url = API_BASE + `/reg/insertPatient?user_id=${encodeURIComponent(user_id)}&user_name=${encodeURIComponent(user_name)}&password=${encodeURIComponent(password)}&email=${encodeURIComponent(email)}&detectionAreaType=${encodeURIComponent(detectionAreaType)}`;
    } else {
        url = API_BASE + `/reg/insertManager?user_id=${encodeURIComponent(user_id)}&user_name=${encodeURIComponent(user_name)}&password=${encodeURIComponent(password)}&email=${encodeURIComponent(email)}`;
    }
    fetch(url, { method: 'POST' })
        .then(res => res.text())
        .then(text => {
            if (text.includes('1') || text.includes('회원가입 성공')) {
                Swal.fire({
                    icon: 'success',
                    title: '회원가입이 완료되었습니다',
                    confirmButtonText: '로그인',
                }).then((result) => {
                    if (result.isConfirmed) {
                        window.location.href = '/user/sign-in.html';
                    }
                });
            } else {
                document.getElementById('msg').textContent = text;
            }
        })
        .catch(() => { document.getElementById('msg').textContent = '회원가입 요청 실패'; });
}
// 아이디 중복 확인
const userIdInput = document.getElementById('user_id');
const checkIdBtn = document.getElementById('checkIdBtn');
const idCheckMsg = document.getElementById('idCheckMsg');
let idCheckTimeout = null;
checkIdBtn.onclick = function() {
    const userId = userIdInput.value.trim();
    if (!userId) {
        Swal.fire({
            icon: 'warning',
            text: '아이디를 입력하세요.'
        });
        userIdInput.focus();
        return;
    }
    // 중복 확인 요청
    fetch(API_BASE + `/reg/checkUserId?user_id=${encodeURIComponent(userId)}`)
        .then(res => res.json())
        .then(data => {
            if (data.available) {
                idCheckMsg.textContent = '사용 가능한 아이디입니다.'; idCheckMsg.className = 'subtext ok';
                userIdInput.classList.add('valid');
            } else {
                idCheckMsg.textContent = '이미 사용 중인 아이디입니다.'; idCheckMsg.className = 'subtext err';
                userIdInput.classList.remove('valid');
            }
        })
        .catch(() => { idCheckMsg.textContent = '아이디 중복 확인 실패'; idCheckMsg.className = 'subtext err'; });
};

// 폼 submit 막기
regForm.onsubmit = e => { e.preventDefault(); };
// 엔터 키 입력 시 단계별 제어
regForm.addEventListener('keydown', function(e) {
    if (e.key === 'Enter') {
        // 현재 단계가 마지막 단계(회원가입) 전이면 submit 막고 다음 단계로만 이동
        if (currentStep < steps.length - 1) {
            e.preventDefault();
            nextStep();
        }
        // 회원가입 단계에서는 엔터로 submit 허용
    }
});
// 첫 화면 표시
showStep(currentStep);

// 약관 동의 관련 코드
const agreeAllCheckbox = document.getElementById('agreeAll');
const agreementCheckboxes = document.querySelectorAll('.agreement-checkbox');

// 보호자용 약관 동의 관련 코드 추가
const agreeAllManagerCheckbox = document.getElementById('agreeAllManager');
const agreementManagerCheckboxes = document.querySelectorAll('.agreement-checkbox-manager');

// 전체 동의 체크박스 이벤트 (환자용)
agreeAllCheckbox.addEventListener('change', function() {
    agreementCheckboxes.forEach(checkbox => {
        checkbox.checked = this.checked;
    });
});

// 개별 체크박스 변경 시 전체 동의 상태 업데이트 (환자용)
agreementCheckboxes.forEach(checkbox => {
    checkbox.addEventListener('change', function() {
        agreeAllCheckbox.checked = Array.from(agreementCheckboxes).every(cb => cb.checked);
    });
});

// 전체 동의 체크박스 이벤트 (보호자용)
agreeAllManagerCheckbox.addEventListener('change', function() {
    agreementManagerCheckboxes.forEach(checkbox => {
        checkbox.checked = this.checked;
    });
});

// 개별 체크박스 변경 시 전체 동의 상태 업데이트 (보호자용)
agreementManagerCheckboxes.forEach(checkbox => {
    checkbox.addEventListener('change', function() {
        agreeAllManagerCheckbox.checked = Array.from(agreementManagerCheckboxes).every(cb => cb.checked);
    });
});

// 회원가입 버튼 클릭 시 약관 동의 검증 추가
const originalSubmitForm = submitForm;
submitForm = function() {
    // 환자 회원가입 약관 동의 확인
    if (role === 'patient' && currentStep === 3) {
        const termsAgreed = document.getElementById('agreeTerms').checked;
        const privacyAgreed = document.getElementById('agreePrivacy').checked;

        if (!termsAgreed || !privacyAgreed) {
            Swal.fire({
                icon: 'warning',
                text: '필수 약관에 동의해주세요.'
            });
            return;
        }
    }

    // 보호자 회원가입 약관 동의 확인
    if (role === 'manager' && currentStep === 2) {
        const termsAgreed = document.getElementById('agreeTermsManager').checked;
        const privacyAgreed = document.getElementById('agreePrivacyManager').checked;

        if (!termsAgreed || !privacyAgreed) {
            Swal.fire({
                icon: 'warning',
                text: '필수 약관에 동의해주세요.'
            });
            return;
        }
    }

    // 기존 submitForm 함수 실행
    originalSubmitForm();
};

// 약관 보기 버튼 클릭 시 모달 표시
document.getElementById('viewTerms').onclick = function() {
    showTerms('terms');
};
document.getElementById('viewPrivacy').onclick = function() {
    showTerms('privacy');
};
document.getElementById('viewTermsManager').onclick = function() {
    showTerms('terms');
};
document.getElementById('viewPrivacyManager').onclick = function() {
    showTerms('privacy');
};

function showTerms(type) {
    const modalTitle = document.getElementById('modalTitle');
    const modalContent = document.getElementById('modalContent');

    if (type === 'terms') {
        modalTitle.textContent = '이용약관';
        modalContent.innerHTML = `
                <h4>㈜우장산 산신령들 CueCode 서비스 이용약관</h4>
                <p><strong>제1장 총칙</strong></p>
                <p><strong>제1조 [목적]</strong></p>
                <p>본 약관은 ㈜우장산 산신령들(이하 '회사')이 제공하는 CueCode 서비스(웹 기반 서비스 및 의료기관 납품 서비스 등)의 이용조건 및 절차, 회원과 회사의 권리·의무와 책임사항을 규정함을 목적으로 합니다.</p>

                <p><strong>제2조 [정의]</strong></p>
                <ul>
                <li>콘텐츠 : 회사가 제공하는 제스처 인식 기반 의사소통 보조 자료 일체.</li>
                <li>회원 : 회사와 이용계약을 체결하고 서비스를 이용하는 개인 사용자.</li>
                <li>의료기관 회원 : 병원·재활센터·복지기관 등 단체 계약을 통해 서비스를 이용하는 기관 소속 사용자.</li>
                <li>계정 : 회원 식별을 위해 발급되는 고유한 로그인 수단.</li>
                <li>게시물 : 회원이 서비스 내에서 작성·업로드한 텍스트, 이미지, 영상 등 자료.</li>
                <li>민감정보 : 의료·건강 관련 정보 등 「개인정보보호법」상 특별히 보호되는 정보.</li>
                </ul>

                <p><strong>제2장 회원</strong></p>
                <p><strong>제3조 [회원 가입]</strong></p>
                <p>회원은 회사가 제공하는 웹사이트에서 다음 정보를 입력하여 가입할 수 있습니다.</p>
                <ul>
                <li>이름</li>
                <li>휴대폰번호</li>
                <li>아이디</li>
                <li>비밀번호</li>
                <li>이메일</li>
                <li>감지 범위(서비스 사용 시 카메라·센서의 인식 범위 설정 정보)</li>
                </ul>
                <p>회원은 가입 시 정확한 정보를 기재해야 하며, 허위 정보를 기재할 경우 서비스 이용에 제한을 받을 수 있습니다.</p>
                <p>만 14세 미만의 아동은 법정대리인의 동의를 받아야 하며, 의료·건강정보 수집이 필요한 경우 별도의 동의 절차를 거칩니다.</p>
                <p>회원 가입 완료 시 전용 계정과 저장 공간이 생성됩니다.</p>

                <p><strong>제4조 [회원의 의무]</strong></p>
                <p>회원은 서비스 이용 시 다음 행위를 해서는 안 됩니다.</p>
                <ul>
                <li>타인의 정보 도용</li>
                <li>콘텐츠의 무단 복제·배포</li>
                <li>서비스의 정상 운영을 방해하는 행위</li>
                <li>법령 위반 및 사회질서 저해 행위</li>
                </ul>
                <p>위반 시 회사는 이용 제한 또는 계약 해지 조치를 할 수 있습니다.</p>

                <p><strong>제3장 서비스</strong></p>
                <p><strong>제5조 [서비스 제공]</strong></p>
                <p>회사는 회원 가입 승인 시 서비스를 개시합니다.</p>
                <p>서비스는 원칙적으로 연중무휴, 1일 24시간 제공되나 기술적 사유로 일시 중단될 수 있습니다.</p>
                <p>서비스는 개인적·비상업적 용도로만 이용할 수 있으며, 회사의 사전 허가 없이 영리 목적 이용은 금지됩니다.</p>

                <p><strong>제6조 [의료기관 납품 특칙]</strong></p>
                <p>회사는 병원·재활센터·복지기관과 별도 계약을 통해 CueCode 서비스를 납품할 수 있습니다.</p>
                <p>기관 납품 시 데이터 보안, 개인정보 처리, 장애 대응 수준은 별도의 계약(DPA, SLA)에 따릅니다.</p>
                <p>국외 기관 도입 시 GDPR, HIPAA 등 국제 기준을 준수합니다.</p>

                <p><strong>제4장 개인정보 및 민감정보</strong></p>
                <p><strong>제7조 [개인정보 보호]</strong></p>
                <p>회사는 회원가입 시 수집하는 정보(이름, 휴대폰번호, 아이디, 비밀번호, 이메일, 감지 범위)와 서비스 제공 과정에서 필요한 최소한의 개인정보만 수집합니다.</p>
                <p>의료·건강 등 민감정보는 별도의 동의를 받아 수집하며, 보안 체계를 갖춘 환경에서 관리합니다.</p>
                <p>수집된 정보는 통계·연구 목적으로 비식별화 처리 후 활용할 수 있습니다.</p>
                <p>법률에 의한 경우를 제외하고는 제3자에게 제공하지 않습니다.</p>

                <p><strong>제5장 책임 및 분쟁 해결</strong></p>
                <p><strong>제8조 [책임 제한]</strong></p>
                <p>천재지변, 네트워크 장애 등 회사의 통제 범위를 벗어난 사유에 대해서는 책임을 지지 않습니다.</p>
                <p>회원의 귀책사유로 발생한 문제에 대해서는 회사가 책임을 지지 않습니다.</p>

                <p><strong>제9조 [분쟁 해결]</strong></p>
                <p>회사와 회원 간 분쟁은 성실히 협의하여 해결합니다.</p>
                <p>협의가 이루어지지 않을 경우 「콘텐츠분쟁조정위원회」 또는 「의료분쟁조정위원회」에 조정을 신청할 수 있습니다.</p>
                <p>최종적으로는 관할 법원에 소를 제기할 수 있습니다.</p>

                <p><strong>[부칙]</strong></p>
                <p>본 약관은 2025년 10월 31일부터 시행합니다.</p>
                <p>종전의 약관은 본 약관으로 대체합니다.</p>
            `;
    } else if (type === 'privacy') {
        modalTitle.textContent = '개인정보 수집 및 이용 동의';
        modalContent.innerHTML = `
                <section class="privacy-consent">
                  <h2>[필수] 개인정보 수집 및 이용 동의</h2>
                  <p>
                    ㈜우장산 산신령들(이하 "회사")는 CueCode 서비스 제공을 위해 필요한 최소한의 개인정보를 수집하고 있습니다.
                  </p>

                  <div class="privacy-table">
                    <div class="privacy-table-row" style="display: grid; grid-template-columns: 20% 50% 30%; background-color: #f1f5f9; font-weight: bold; border: 1px solid #ccc;">
                      <div class="privacy-table-cell" style="padding: 8px; border-right: 1px solid #ccc;">목적</div>
                      <div class="privacy-table-cell" style="padding: 8px; border-right: 1px solid #ccc;">수집항목</div>
                      <div class="privacy-table-cell" style="padding: 8px;">보유·이용 기간</div>
                    </div>

                    <div class="privacy-table-row" style="display: grid; grid-template-columns: 20% 50% 30%; border: 1px solid #ccc; border-top: none;">
                      <div class="privacy-table-cell" style="padding: 8px; border-right: 1px solid #ccc;">
                        회원 가입<br>(회원 관리, 서비스 제공 및 기관 계약 이행, 신규 서비스 안내 활용)
                      </div>
                      <div class="privacy-table-cell" style="padding: 8px; border-right: 1px solid #ccc;">
                        - [필수] 이름, 아이디, 비밀번호, 이메일<br>
                        - [필수] 감지 범위 설정 값 (서비스 내 제스처 인식 센서 범위 설정 정보)<br>
                        - [필수] 사용자별 매핑 언어 (제스처와 매핑된 문장 및 사용자가 선택한 언어 설정 값)
                      </div>
                      <div class="privacy-table-cell" style="padding: 8px;">
                        - 회원 탈퇴 시 지체 없이 파기<br>
                        - 단, 회원 가입 이력 확인 및 부정 이용 방지를 위하여 탈퇴일로부터 1년 동안 암호화하여 보관 후 파기<br>
                        - 관계 법령에 따라 일정기간 보존이 필요한 경우 해당 법령에 따름
                      </div>
                    </div>

                    <div class="privacy-table-row" style="display: grid; grid-template-columns: 20% 50% 30%; border: 1px solid #ccc; border-top: none;">
                      <div class="privacy-table-cell" style="padding: 8px; border-right: 1px solid #ccc;">
                        서비스 운영 및 안전 관리
                      </div>
                      <div class="privacy-table-cell" style="padding: 8px; border-right: 1px solid #ccc;">
                        기기정보(브라우저, OS, 접속 IP), 접속 로그, 쿠키
                      </div>
                      <div class="privacy-table-cell" style="padding: 8px;">
                        - 회원 탈퇴 시 지체 없이 파기<br>
                        - 단, 서비스 운영 보안 목적의 기록은 3개월 보관 후 파기
                      </div>
                    </div>
                  </div>

                  <h3>안내사항</h3>
                  <ul>
                    <li>회사는 서비스 운영 과정에서 기기정보, 접속 로그, 쿠키 등이 자동으로 수집될 수 있습니다.</li>
                    <li>회사는 제스처 인식 과정에서 카메라를 활용하지만,
                        <strong>영상 데이터는 저장되지 않으며</strong> 실시간 인식 목적에 한해 일시적으로 처리됩니다.</li>
                    <li>귀하는 개인정보 수집 및 이용에 동의하지 않을 권리가 있으나, 동의 거부 시 서비스 이용이 제한될 수 있습니다.</li>
                    <li>의료기관 납품용 서비스의 경우, 기관 계약에 따라 별도의 개인정보처리방침 및 보안 규정이 추가 적용될 수 있습니다.</li>
                  </ul>
                </section>
            `;
    }
    document.getElementById('termsModal').style.display = 'block';
}


function closeModal() {
    document.getElementById('termsModal').style.display = 'none';
}

// 로그인/비밀번호 찾기 버튼 이벤트
document.getElementById('goLoginBtn').onclick = function() {
    window.location.href = '/login';
};
document.getElementById('goFindPwBtn').onclick = function() {
    window.location.href = '/find-password';
};

// 프로세싱 바 업데이트 함수
function updateProgressBar(step) {
    const steps = [
        document.getElementById('step1-bar'),
        document.getElementById('step2-bar'),
        document.getElementById('step3-bar'),
        document.getElementById('step4-bar')
    ];
    const lines = document.querySelectorAll('.progress-bar-line');
    steps.forEach((el, idx) => {
        if (el) {
            if (idx === step - 1) {
                el.classList.add('active');
                el.querySelector('.circle').style.background = '#6FC8F8';
                el.querySelector('.circle').style.color = '#fff';
            } else {
                el.classList.remove('active');
                el.querySelector('.circle').style.background = '#e0e0e0';
                el.querySelector('.circle').style.color = '#6FC8F8';
            }
        }
    });
    // 보호자일 때 4단계와 이메일 인증 오른쪽 라인(lines[2]) 숨김, 환자일 때 보임
    if (role === 'manager') {
        if (steps[3]) steps[3].style.display = 'none';
        if (lines[2]) lines[2].style.display = 'none';
    } else {
        if (steps[3]) steps[3].style.display = '';
        if (lines[2]) lines[2].style.display = '';
    }
}