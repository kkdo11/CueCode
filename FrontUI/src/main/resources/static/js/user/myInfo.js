// 마스킹 함수
function maskName(name) {
    if (!name) return '';
    return name[0] + '*'.repeat(Math.max(0, name.length - 1));
}
function maskId(id) {
    if (!id) return '';
    if (id.length <= 4) return id[0] + '***' + id[id.length-1];
    return id.slice(0,2) + '***' + id.slice(-2);
}
function maskEmail(email) {
    if (!email) return '이메일 정보 없음';
    const [local, domain] = email.split('@');
    if (!local || !domain) return email;
    return local.slice(0,2) + '***@' + domain;
}
document.addEventListener('DOMContentLoaded', function() {
    let userId = '';
    let currentPatientId = '';
    const token = document.cookie.split('; ').find(row => row.startsWith('jwtAccessToken='));
    if (token) {
        try {
            const decoded = jwt_decode(token.split('=')[1]);
            userId = decoded.sub || decoded.userId || '';
        } catch (e) {
            userId = '';
        }
    }
    console.log('[프론트] userId:', userId); // userId 추출 직후 로그 추가
    // 사용자 정보 API로 정보 표시
    if (userId) {
        fetch('http://localhost:13000/user/info?userId=' + encodeURIComponent(userId), {
            method: 'GET',
            credentials: 'include'
        })
            .then(res => res.json())
            .then(data => {
                console.log('[프론트] user info 응답:', data); // user info 응답 로그 추가
                document.getElementById('info-name').textContent = maskName(data.name || data.userName || '');
                document.getElementById('info-id').textContent = maskId(data.id || data.userId || userId);
                document.getElementById('info-email').textContent = maskEmail(data.email || '');
                // 감지 범위 표시: 환자 회원이면 별도 API 호출
                const detectionAreaRow = document.getElementById('detection-area-row');
                const detectionAreaParent = detectionAreaRow;
                if (data.userType === 'patient') {
                    console.log('[프론트] 감지 범위 조회 요청 body:', { patientId: userId });
                    fetch('http://localhost:13000/patient/detection-area/read', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ patientId: userId })
                    })
                        .then(res => res.json())
                        .then(area => {
                            console.log('[프론트] 감지 범위 조회 응답:', area);
                            if (area.result === 1) {
                                const areaText = area.detectionArea === 'hand' ? '손' : area.detectionArea === 'face' ? '얼굴' : area.detectionArea === 'both' ? '손과 얼굴' : '설정 안됨';
                                document.getElementById('detection-area-value').textContent = areaText;
                                detectionAreaParent.classList.remove('d-none');
                            } else {
                                document.getElementById('detection-area-value').textContent = '설정 안됨';
                                detectionAreaParent.classList.remove('d-none');
                            }
                        })
                        .catch((err) => {
                            console.error('[프론트] 감지 범위 조회 fetch 에러:', err);
                            document.getElementById('detection-area-value').textContent = '조회 오류';
                            detectionAreaParent.classList.remove('d-none');
                        });
                } else {
                    detectionAreaParent.classList.add('d-none');
                }
            });
    }
    // 이름 변경 버튼
    document.getElementById('changeNameBtn').onclick = function() {
        document.getElementById('changeNameMsg').textContent = '';
        document.getElementById('newNameInput').value = '';
        new bootstrap.Modal(document.getElementById('changeNameModal')).show();
    };
    document.getElementById('saveNameBtn').onclick = async function() {
        const newName = document.getElementById('newNameInput').value.trim();
        if (!newName) {
            document.getElementById('changeNameMsg').textContent = '새 이름을 입력하세요.';
            return;
        }
        // 이름 변경 API 호출
        const res = await fetch('http://localhost:13000/user/update-name', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId, newName }),
            credentials: 'include'
        });
        const data = await res.json();
        if (res.ok && data.result === 1) {
            document.getElementById('info-name').textContent = maskName(newName);
            Swal.fire({ icon: 'success', text: '이름이 변경되었습니다.', timer: 1200, showConfirmButton: false });
            bootstrap.Modal.getInstance(document.getElementById('changeNameModal')).hide();
        } else {
            document.getElementById('changeNameMsg').textContent = data.msg || '이름 변경 실패';
        }
    };
    // 아이디 변경 버튼
    document.getElementById('changeIdBtn').onclick = function() {
        document.getElementById('changeIdMsg').textContent = '';
        document.getElementById('newIdInput').value = '';
        new bootstrap.Modal(document.getElementById('changeIdModal')).show();
    };
    document.getElementById('saveIdBtn').onclick = async function() {
        const newId = document.getElementById('newIdInput').value.trim();
        if (!newId) {
            document.getElementById('changeIdMsg').textContent = '새 아이디를 입력하세요.';
            return;
        }
        // 아이디 변경 API 호출
        const res = await fetch('http://localhost:13000/user/update-id', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId, newId }),
            credentials: 'include'
        });
        const data = await res.json();
        if (res.ok && data.result === 1) {
            document.getElementById('info-id').textContent = maskId(newId);
            Swal.fire({ icon: 'success', text: '아이디가 변경되었습니다.', timer: 1200, showConfirmButton: false });
            bootstrap.Modal.getInstance(document.getElementById('changeIdModal')).hide();
            userId = newId;
        } else {
            document.getElementById('changeIdMsg').textContent = data.msg || '아이디 변경 실패';
        }
    };
    // 이메일 변경 버튼
    document.getElementById('changeEmailBtn').onclick = function() {
        document.getElementById('changeEmailMsg').textContent = '';
        document.getElementById('newEmailInput').value = '';
        document.getElementById('emailCodeInput').value = '';
        new bootstrap.Modal(document.getElementById('changeEmailModal')).show();
    };
    let emailChangeVerified = false, emailChangeCountdown = null, emailChangeRemaining = 0;
    function formatTime(sec){ const m = Math.floor(sec/60).toString().padStart(2,'0'); const s = (sec%60).toString().padStart(2,'0'); return `${m}:${s}`; }
    function startEmailChangeCountdown(seconds){
        emailChangeRemaining = seconds;
        document.getElementById('changeEmailMsg').textContent = `인증번호 입력 (${formatTime(emailChangeRemaining)})`;
        clearInterval(emailChangeCountdown);
        emailChangeCountdown = setInterval(()=>{
            emailChangeRemaining--;
            document.getElementById('changeEmailMsg').textContent = `인증번호 입력 (${formatTime(Math.max(emailChangeRemaining,0))})`;
            if(emailChangeRemaining<=0){
                clearInterval(emailChangeCountdown);
                document.getElementById('changeEmailMsg').textContent = '인증 시간이 만료되었습니다.';
                document.getElementById('sendEmailCodeBtn').disabled = false;
                document.getElementById('sendEmailCodeBtn').textContent = '인증번호 재발송';
            }
        },1000);
    }
    // 이메일 인증번호 발송
    document.getElementById('sendEmailCodeBtn').onclick = function() {
        const email = document.getElementById('newEmailInput').value.trim();
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            document.getElementById('changeEmailMsg').textContent = '올바른 이메일 주소를 입력하세요.';
            return;
        }
        fetch('http://localhost:13000/reg/sendMail', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email })
        })
            .then(res => res.json())
            .then(data => {
                if (data.result === 1) {
                    startEmailChangeCountdown(180);
                    document.getElementById('emailCodeInput').value = '';
                    document.getElementById('emailCodeInput').focus();
                    document.getElementById('sendEmailCodeBtn').textContent = '발송 완료';
                    document.getElementById('sendEmailCodeBtn').disabled = true;
                    Swal.fire({ icon: 'success', text: '인증메일이 발송되었습니다. 메일을 확인하세요.' });
                } else {
                    Swal.fire({ icon: 'error', text: '메일 발송 실패: ' + data.msg });
                }
            })
            .catch(() => {
                Swal.fire({ icon: 'error', text: '메일 발송 요청 실패' });
            });
    };
    document.getElementById('emailCodeInput').addEventListener('input', function() {
        document.getElementById('changeEmailMsg').textContent = '';
    });
    document.getElementById('emailCodeInput').addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            verifyChangeEmailCode();
        }
    });
    function verifyChangeEmailCode() {
        const email = document.getElementById('newEmailInput').value.trim();
        const authCode = document.getElementById('emailCodeInput').value.trim();
        fetch('http://localhost:13000/reg/verifyEmailAuth', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email, authCode: authCode })
        })
            .then(res => res.json())
            .then(data => {
                if (data.result === 1) {
                    clearInterval(emailChangeCountdown);
                    emailChangeVerified = true;
                    document.getElementById('changeEmailMsg').textContent = '이메일 인증이 완료되었습니다.';
                    document.getElementById('sendEmailCodeBtn').disabled = true;
                    document.getElementById('sendEmailCodeBtn').textContent = '인증 완료';
                    // 인증 성공 시 모달 자동 닫기
                    setTimeout(function() {
                        bootstrap.Modal.getInstance(document.getElementById('changeEmailModal')).hide();
                    }, 700);
                } else {
                    emailChangeVerified = false;
                    document.getElementById('changeEmailMsg').textContent = data.msg || '인증번호가 일치하지 않습니다.';
                }
            })
            .catch(() => {
                emailChangeVerified = false;
                document.getElementById('changeEmailMsg').textContent = '서버 인증 요청 실패. 다시 시도해주세요.';
            });
    }
    document.getElementById('emailCodeInput').addEventListener('blur', verifyChangeEmailCode);
    // 이메일 변경 저장 시 인증 완료 여부 체크
    const originalSaveEmailBtn = document.getElementById('saveEmailBtn').onclick;
    document.getElementById('saveEmailBtn').onclick = function() {
        if (!emailChangeVerified) {
            document.getElementById('changeEmailMsg').textContent = '이메일 인증을 완료하세요.';
            return;
        }
        if (typeof originalSaveEmailBtn === 'function') originalSaveEmailBtn();
    };
    // 비밀번호 변경 버튼
    document.getElementById('changePwBtn').onclick = function() {
        document.getElementById('changePwMsg').textContent = '';
        document.getElementById('currentPwInput').value = '';
        document.getElementById('newPwInput').value = '';
        document.getElementById('confirmPwInput').value = '';
        new bootstrap.Modal(document.getElementById('changePwModal')).show();
    };
    document.getElementById('savePwBtn').onclick = async function() {
        const currentPw = document.getElementById('currentPwInput').value.trim();
        const newPw = document.getElementById('newPwInput').value.trim();
        const confirmPw = document.getElementById('confirmPwInput').value.trim();
        if (!currentPw || !newPw || !confirmPw) {
            document.getElementById('changePwMsg').textContent = '모든 항목을 입력하세요.';
            return;
        }
        if (newPw !== confirmPw) {
            document.getElementById('changePwMsg').textContent = '새 비밀번호가 일치하지 않습니다.';
            return;
        }
        // 비밀번호 변경 API 호출
        const res = await fetch('http://localhost:13000/user/update-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId, currentPassword: currentPw, newPassword: newPw }),
            credentials: 'include'
        });
        const data = await res.json();
        if (res.ok && data.result === 1) {
            Swal.fire({ icon: 'success', text: '비밀번호가 변경되었습니다.', timer: 1200, showConfirmButton: false });
            bootstrap.Modal.getInstance(document.getElementById('changePwModal')).hide();
        } else {
            document.getElementById('changePwMsg').textContent = data.msg || '비밀번호 변경 실패';
        }
    };
    // 감지 범위 변경 버튼
    document.getElementById('changeDetectionAreaBtn').onclick = function() {
        new bootstrap.Modal(document.getElementById('changeDetectionAreaModal')).show();
    };
    document.getElementById('saveDetectionAreaBtnMain').onclick = function() {
        window.location.href = 'http://localhost:14000/index.html';
    };
    document.getElementById('saveDetectionAreaBtnModal').onclick = async function() {
        if (!userId || userId.trim() === '') {
            document.getElementById('changeDetectionAreaMsg').textContent = '환자 정보가 올바르게 로드되지 않았습니다.';
            console.error('감지 범위 변경 시 userId 값:', userId);
            return;
        }
        const detectionAreaType = document.querySelector('input[name="detectionAreaType"]:checked');
        if (!detectionAreaType) {
            document.getElementById('changeDetectionAreaMsg').textContent = '변경할 감지 범위를 선택하세요.';
            return;
        }
        const newDetectionArea = detectionAreaType.value;
        // 감지 범위 변경 API 호출
        const res = await fetch('http://localhost:13000/patient/detection-area/update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ patientId: userId, detectionAreaType: newDetectionArea })
        });
        const data = await res.json();
        if (res.ok && data.result === 1) {
            const areaText = newDetectionArea === 'hand' ? '손' : newDetectionArea === 'face' ? '얼굴' : '손과 얼굴';
            document.getElementById('detection-area-value').textContent = areaText;
            Swal.fire({ icon: 'success', text: '감지 범위가 변경되었습니다.', timer: 1200, showConfirmButton: false });
            bootstrap.Modal.getInstance(document.getElementById('changeDetectionAreaModal')).hide();
        } else {
            document.getElementById('changeDetectionAreaMsg').textContent = data.msg || '감지 범위 변경 실패';
        }
    };
    // 모달 닫힘 시 포커스 해제
    document.getElementById('changeDetectionAreaModal').addEventListener('hidden.bs.modal', function() {
        if (document.activeElement && document.activeElement.id === 'detectHand') {
            document.activeElement.blur();
        }
    });
    // JWT 토큰을 쿠키에서 꺼내는 함수 추가
    function getCookie(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return null;
    }
    document.getElementById('withdrawalBtn').onclick = function(e) {
        e.preventDefault();
        Swal.fire({
            title: '정말 탈퇴하시겠습니까?',
            text: '탈퇴 후 복구가 불가능합니다.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: '네, 탈퇴합니다',
            cancelButtonText: '아니오'
        }).then((result) => {
            if (result.isConfirmed) {
                const token = getCookie('jwtAccessToken');
                fetch('http://localhost:13000/user/withdrawal', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
                    },
                    credentials: 'include',
                    body: JSON.stringify({ userId })
                })
                    .then(res => res.json())
                    .then(data => {
                        if (data.result === 1) {
                            Swal.fire({ icon: 'success', text: '정상적으로 탈퇴되었습니다.', timer: 1500, showConfirmButton: false });
                            setTimeout(function() {
                                window.location.href = '/user/sign-in.html';
                            }, 1500);
                        } else {
                            Swal.fire({ icon: 'error', text: data.msg || '탈퇴에 실패했습니다.' });
                        }
                    })
                    .catch(() => {
                        Swal.fire({ icon: 'error', text: '서버 오류로 탈퇴에 실패했습니다.' });
                    });
            }
        });
    };
});