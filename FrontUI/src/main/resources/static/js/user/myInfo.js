
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

async function initMyInfoPage() {
    let userId = '';
    let userRole = '';

    // 1. /user/me API로 기본 사용자 정보(ID, 역할) 가져오기
    try {
        const meResponse = await fetch(API_BASE + '/user/me', {
            method: 'GET',
            credentials: 'include'
        });

        if (meResponse.ok) {
            const meData = await meResponse.json();
            userId = meData.userId;
            userRole = meData.userRole;
            console.log('[프론트] /user/me 응답 userId:', userId);
        } else {
            throw new Error('사용자 인증 정보를 가져오는데 실패했습니다.');
        }
    } catch (error) {
        console.error('[프론트] /user/me API 호출 오류:', error);
        Swal.fire({
            icon: 'error',
            text: '사용자 정보를 가져올 수 없습니다. 다시 로그인해주세요.',
            confirmButtonText: '로그인으로 이동'
        }).then(() => {
            window.location.href = '/user/sign-in.html';
        });
        return; // 함수 실행 중단
    }

    // 2. userId로 상세 정보 조회
    if (userId) {
        try {
            const infoResponse = await fetch(API_BASE + '/user/info?userId=' + encodeURIComponent(userId), {
                method: 'GET',
                credentials: 'include'
            });
            const infoData = await infoResponse.json();

            console.log('[프론트] /user/info 응답:', infoData);
            console.log('[프론트] infoData.userType:', infoData.userType);
            document.getElementById('info-name').textContent = maskName(infoData.name || '');
            document.getElementById('info-id').textContent = maskId(infoData.id || userId);
            document.getElementById('info-email').textContent = maskEmail(infoData.email || '');

            // 감지 범위 표시 (환자일 경우)
            const detectionAreaRow = document.getElementById('detection-area-row');
            console.log('[프론트] JS 실행 전 detectionAreaRow hidden 상태:', detectionAreaRow.hasAttribute('hidden'));
            if (infoData.userType === 'patient') {
                console.log('[프론트] userType이 patient입니다. detectionAreaRow의 hidden 속성을 제거합니다.');
                detectionAreaRow.removeAttribute('hidden'); // 환자일 경우 hidden 속성 제거
                detectionAreaRow.classList.add('d-flex', 'flex-row', 'align-items-center', 'justify-content-between');

                // 감지 범위 조회 및 표시 로직
                try {
                    const daResponse = await fetch(API_BASE + '/user/detection-area?userId=' + encodeURIComponent(userId), {
                        method: 'GET',
                        credentials: 'include'
                    });
                    const daData = await daResponse.json();
                    console.log('[프론트] 감지 범위 응답:', daData);

                    let areaText = '없음';
                    if (daData.hand && daData.face) {
                        areaText = '손과 얼굴';
                        document.getElementById('detectBoth').checked = true;
                    } else if (daData.hand) {
                        areaText = '손';
                        document.getElementById('detectHand').checked = true;
                    } else if (daData.face) {
                        areaText = '얼굴';
                        document.getElementById('detectFace').checked = true;
                    }
                    document.getElementById('detection-area-value').textContent = areaText;
                } catch (daError) {
                    console.error('[프론트] 감지 범위 API 호출 오류:', daError);
                    document.getElementById('detection-area-value').textContent = '정보 로드 실패';
                }

                // 감지 범위 변경 버튼
                document.getElementById('changeDetectionAreaBtn').onclick = function() {
                    document.getElementById('changeDetectionAreaMsg').textContent = '';
                    new bootstrap.Modal(document.getElementById('changeDetectionAreaModal')).show();
                };

                // 감지 범위 저장 버튼 (모달 내부)
                document.getElementById('saveDetectionAreaBtnModal').onclick = async function() {
                    const selectedArea = document.querySelector('input[name="detectionAreaType"]:checked');
                    if (!selectedArea) {
                        document.getElementById('changeDetectionAreaMsg').textContent = '감지 범위를 선택해주세요.';
                        return;
                    }
                    const detectionAreaType = selectedArea.value;
                    const res = await fetch(API_BASE + '/user/update-detection-area', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ userId, detectionAreaType }),
                        credentials: 'include'
                    });
                    const data = await res.json();

                    if (res.ok && data.result === 1) {
                        let areaText = '';
                        if (detectionAreaType === 'hand') areaText = '손';
                        else if (detectionAreaType === 'face') areaText = '얼굴';
                        else if (detectionAreaType === 'both') areaText = '손과 얼굴';
                        document.getElementById('detection-area-value').textContent = areaText;
                        Swal.fire({ icon: 'success', text: '감지 범위가 변경되었습니다.', timer: 1200, showConfirmButton: false });
                        bootstrap.Modal.getInstance(document.getElementById('changeDetectionAreaModal')).hide();
                    } else {
                        document.getElementById('changeDetectionAreaMsg').textContent = data.msg || '감지 범위 변경 실패';
                    }
                };

            } else {
                // 환자가 아닐 경우 감지 범위 행 숨김 (명시적)
                console.log('[프론트] userType이 patient가 아닙니다. detectionAreaRow를 숨깁니다.');
                detectionAreaRow.setAttribute('hidden', ''); // hidden 속성 유지
            }
            console.log('[프론트] 최종 detectionAreaRow hidden 상태:', detectionAreaRow.hasAttribute('hidden'));
        } catch (error) {
            console.error('[프론트] /user/info API 호출 오류:', error);
        }
    }


    // --- 이하 이벤트 핸들러들은 모두 이 클로저 내에서 `userId`를 안전하게 사용 ---

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
        const res = await fetch(API_BASE + '/user/update-name', {
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
        const res = await fetch(API_BASE + '/user/update-id', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId, newId }),
            credentials: 'include'
        });
        const data = await res.json();
        if (res.ok && data.result === 1) {
            document.getElementById('info-id').textContent = maskId(newId);
            Swal.fire({ icon: 'success', text: '아이디가 변경되었습니다. 다시 로그인해주세요.', timer: 1200, showConfirmButton: false });
            // 중요: ID 변경 후에는 재로그인이 필요하므로 로그아웃 처리 또는 로그인 페이지로 리디렉션
            setTimeout(() => window.location.href = '/user/sign-in.html', 1200);
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
        fetch(API_BASE + '/reg/sendMail', {
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
        fetch(API_BASE + '/reg/verifyEmailAuth', {
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
    document.getElementById('verifyEmailCodeBtn').onclick = verifyChangeEmailCode;
    // 이메일 변경 저장 시 인증 완료 여부 체크
    document.getElementById('saveEmailBtn').onclick = async function() {
        if (!emailChangeVerified) {
            document.getElementById('changeEmailMsg').textContent = '이메일 인증을 완료하세요.';
            return;
        }
        const newEmail = document.getElementById('newEmailInput').value.trim();
        const res = await fetch(API_BASE + '/user/update-email', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId, newEmail }),
            credentials: 'include'
        });
        const data = await res.json();
        if (res.ok && data.result === 1) {
            document.getElementById('info-email').textContent = maskEmail(newEmail);
            Swal.fire({ icon: 'success', text: '이메일이 변경되었습니다.', timer: 1200, showConfirmButton: false });
            bootstrap.Modal.getInstance(document.getElementById('changeEmailModal')).hide();
        } else {
            document.getElementById('changeEmailMsg').textContent = data.msg || '이메일 변경 실패';
        }
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
        const res = await fetch(API_BASE + '/user/update-password', {
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
    
    // 회원 탈퇴 버튼
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
                fetch(API_BASE + '/user/withdrawal', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ userId })
                })
                .then(res => res.json())
                .then(data => {
                    if (data.result === 1) {
                        Swal.fire({ icon: 'success', text: '정상적으로 탈퇴되었습니다.', timer: 1500, showConfirmButton: false });
                        setTimeout(() => window.location.href = '/user/sign-in.html', 1500);
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

    // 메인 페이지로 이동 버튼
    document.getElementById('goToIndexBtn').onclick = function() {
        window.location.href = '/index.html';
    };
}

document.addEventListener('DOMContentLoaded', initMyInfoPage);