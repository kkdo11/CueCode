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
        authBtnGroup.innerHTML = `<span class="me-3 fw-bold text-dark">${userName ? userName + '님' : ''} 환영합니다!</span>
                <button id="logoutBtn" class="btn btn-outline-danger px-4 py-2">로그아웃</button>`;
        document.getElementById('logoutBtn').onclick = function () {
            // 서버 로그아웃 API 호출
            fetch(API_BASE + '/user/v1/logout', {method: 'POST', credentials: 'include'})
                .finally(() => {
                    removeCookie('jwtAccessToken');
                    removeCookie('jwtRefreshToken');
                    window.location.href = 'sign-in.html';
                });
        };
        console.log('로그아웃 버튼 노출 완료');
    }
}

// --- [3] DOMContentLoaded 메인 로직 (헤더 버튼 변경) ---
window.addEventListener('DOMContentLoaded', function () {
    const token = getCookie('jwtAccessToken');

    // 헤더 메뉴 링크 요소
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
        console.log('미로그인 상태: 대시보드 및 마이페이지 메뉴 숨김');
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

        // 역할 클레임 병합: 'roles' 키의 값을 'role' 키로 사용
        decoded.role = decoded.role || decoded.roles;

    } catch (e) {
        console.error('토큰 디코딩 실패. 토큰 만료 또는 오류:', e);
        removeCookie('jwtAccessToken');
        window.location.reload();
        return;
    }

    // 3. 역할(ROLE)에 따른 헤더 UI 변경
    if (decoded && decoded.role) {

        // 모든 메뉴를 일단 숨기고, 역할에 맞는 메뉴만 표시
        if (patientMenuItem) patientMenuItem.style.display = 'none';
        if (managerMenuItem) managerMenuItem.style.display = 'none';
        if (myPageMenuItem) myPageMenuItem.style.display = 'none';

        // 마이페이지는 로그인 시 항상 보이도록 설정
        if (myPageMenuItem && myPageMenuLinkAnchor) {
            myPageMenuLinkAnchor.href = '../user/mypage.html';
            myPageMenuItem.style.display = 'list-item'; // <li> 표시
        }

        if (decoded.role === 'ROLE_USER' && patientMenuItem) {
            // 환자 역할
            patientMenuItem.style.display = 'list-item'; // <li> 표시
        } else if (decoded.role === 'ROLE_USER_MANAGER' && managerMenuItem) {
            // 관리자(보호자) 역할
            managerMenuItem.style.display = 'list-item'; // <li> 표시
        }

        // 헤더의 로그인/로그아웃 버튼 업데이트
        showLogoutButton(decoded.userName);


    } else {
        console.log('로그인 상태지만 역할(role) 정보가 불분명함. 로그아웃 버튼과 마이페이지 버튼만 노출.');

        // 역할 정보가 없어도 로그인 상태이므로 로그아웃 버튼은 보여줍니다.
        if (decoded && decoded.userName) {
            showLogoutButton(decoded.userName);
            // 마이페이지는 보이게 설정
            if (myPageMenuItem) myPageMenuItem.style.display = 'list-item';
        }
    }
});


// 두번째꺼
document.addEventListener('DOMContentLoaded', async () => {
    const patientListBody = document.getElementById('patient-list-tbody');

    // ---- 유틸 ----
    const qs = (sel) => document.querySelector(sel);
    const qid = (id) => document.getElementById(id);
    const show = (el, on = true, cls = 'd-none') => el && el.classList.toggle(cls, !on);
    const text = (id, v) => {
        const el = qid(id);
        if (el) el.textContent = v ?? '';
    };

    // null-safe 이벤트 바인딩
    function on(id, evt, handler, {assign = false} = {}) {
        const el = qid(id);
        if (!el) {
            console.warn(`[bind] not found: #${id}`);
            return;
        }
        if (assign && evt === 'click') el.onclick = handler;
        else el.addEventListener(evt, handler);
    }

    // ---- 역할(임시) ----
    window.userRole = 'ROLE_USER_MANAGER';
    try {
        const me = await fetch(API_BASE + '/user/me', {credentials: 'include'});
        if (me.ok) {
            const data = await me.json();
            console.log('/user/me', data);
            // server가 role 제공하면 아래 주석 해제
            // window.userRole = data.role || data.roles || 'ROLE_USER_MANAGER';
        }
    } catch (_) {
    }

    // ---- API helpers ----
    async function getManagerIdFromAPI() {
        try {
            const res = await fetch(API_BASE + '/user/me', {credentials: 'include'});
            if (!res.ok) return null;
            const data = await res.json();
            return data.managerId ?? null;
        } catch {
            return null;
        }
    }

    // ---- 목록 ----
    async function fetchPatients() {
        try {
            const managerId = await getManagerIdFromAPI();
            console.log('managerId:', managerId);
            if (!managerId) {
                patientListBody.innerHTML = `<tr><td colspan="2" class="text-center text-danger">관리자 정보 없음</td></tr>`;
                return;
            }

            const res = await fetch(API_BASE + `/patient/list?managerId=${encodeURIComponent(managerId)}`, {
                credentials: 'include'
            });

            if (res.status === 403) {
                alert('로그인 세션이 만료되었습니다. 다시 로그인해주세요.');
                location.href = 'sign-in.html';
                return;
            }
            if (!res.ok) throw new Error('list failed');

            const arr = await res.json();
            patientListBody.innerHTML = '';

            if (!Array.isArray(arr) || arr.length === 0) {
                patientListBody.innerHTML = `<tr><td colspan="2" class="text-center text-secondary">표시할 환자가 없습니다.</td></tr>`;
                return;
            }

            arr.forEach(p => {
                if (!p?.id) return;
                const tr = document.createElement('tr');
                tr.innerHTML = `
            <td class="whitespace-nowrap">${p.id}</td>
            <td class="whitespace-nowrap">${p.name ?? ''}</td>
          `;
                tr.addEventListener('click', async () => {
                    try {
                        const d = await fetch(API_BASE + `/patient/detail?id=${encodeURIComponent(p.id)}`, {
                            credentials: 'include'
                        });
                        if (d.ok) {
                            openPatientModal(await d.json());
                        } else {
                            alert('환자 상세 정보를 불러오는 데 실패했습니다.');
                        }
                    } catch (e) {
                        console.error(e);
                        alert('환자 상세 정보를 요청하는 중 오류가 발생했습니다.');
                    }
                });
                patientListBody.appendChild(tr);
            });
        } catch (e) {
            console.error('fetchPatients error:', e);
            patientListBody.innerHTML = `<tr><td colspan="2" class="text-center text-secondary">환자 목록을 불러오지 못했습니다.</td></tr>`;
        }
    }

    // ---- 먼저 불러오기 (중간에 바인딩 에러가 있어도 목록은 뜨게) ----
    fetchPatients().catch(console.error);

    // ---- 환자 추가 ----
    (function setupAdd() {
        const idInput = qid('add-patient-id');
        const addBtn = qid('add-patient-btn');
        const clearBtn = qid('btn-clear');
        const checkBtn = qid('btn-check');

        const resultBox = qid('check-result');
        const resultIcon = qid('check-icon');
        const resultTitle = qid('check-title');
        const resultDesc = qid('check-desc');
        const fillBtn = qid('btn-fill');

        function setLoading(btn, on) {
            const label = btn?.querySelector('.btn-label');
            const wait = btn?.querySelector('.btn-wait');
            if (label && wait) {
                label.classList.toggle('d-none', on);
                wait.classList.toggle('d-none', !on);
            }
            if (btn) btn.disabled = on;
        }

        function validate() {
            const ok = (idInput?.value.trim().length ?? 0) > 0;
            if (addBtn) addBtn.disabled = !ok;
            if (!ok) show(resultBox, false);
            return ok;
        }

        idInput?.addEventListener('input', validate);
        idInput?.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                if (!addBtn?.disabled) addBtn?.click();
            }
        });

        clearBtn?.addEventListener('click', () => {
            if (!idInput) return;
            idInput.value = '';
            validate();
            idInput.focus();
        });

        checkBtn?.addEventListener('click', async () => {
            const id = idInput?.value.trim();
            if (!id) {
                idInput?.focus();
                return;
            }
            show(resultBox, false);
            resultTitle.textContent = '';
            resultDesc.textContent = '';
            resultIcon?.setAttribute('icon', 'solar:clock-circle-linear');

            try {
                const res = await fetch(API_BASE + `/patient/detail?id=${encodeURIComponent(id)}`, {
                    credentials: 'include'
                });
                if (res.ok) {
                    const data = await res.json();
                    resultIcon?.setAttribute('icon', 'solar:shield-check-linear');
                    resultTitle.textContent = `${data.name ?? '이름 미등록'} (ID: ${data.id ?? id})`;
                    // resultDesc.textContent  = `담당 매니저: ${(data.managerIds ?? []).join(', ') || '없음'} · 이메일: ${data.email ?? '정보 없음'}`;
                    resultDesc.textContent = `담당 매니저: ${(data.managerIds ?? []).join(', ') || '없음'}`;

                    show(resultBox, true);
                } else if (res.status === 404) {
                    resultIcon?.setAttribute('icon', 'solar:danger-triangle-linear');
                    resultTitle.textContent = '등록되지 않은 환자입니다.';
                    resultDesc.textContent = 'ID를 확인 후 추가를 진행하세요.';
                    show(resultBox, true);
                } else if (res.status === 403) {
                    await Swal.fire('세션 만료', '다시 로그인 해주세요.', 'warning');
                    location.href = 'sign-in.html';
                } else {
                    resultIcon?.setAttribute('icon', 'solar:danger-triangle-linear');
                    resultTitle.textContent = '조회 실패';
                    resultDesc.textContent = `서버 응답 상태: ${res.status}`;
                    show(resultBox, true);
                }
            } catch {
                resultIcon?.setAttribute('icon', 'solar:danger-triangle-linear');
                resultTitle.textContent = '네트워크 오류';
                resultDesc.textContent = '잠시 후 다시 시도해주세요.';
                show(resultBox, true);
            }
        });

        fillBtn?.addEventListener('click', () => {
            idInput?.focus();
            idInput?.select();
        });

        addBtn?.addEventListener('click', async () => {
            const patientId = idInput?.value.trim();
            if (!patientId) {
                idInput?.focus();
                return;
            }
            setLoading(addBtn, true);
            try {
                const res = await fetch(API_BASE + '/manager/addPatient', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    credentials: 'include',
                    body: JSON.stringify({patientId})
                });
                if (res.ok) {
                    await Swal.fire({
                        icon: 'success',
                        title: '추가 완료',
                        text: `${patientId} 환자가 연결되었습니다.`,
                        timer: 1400,
                        showConfirmButton: false
                    });
                    if (idInput) idInput.value = '';
                    validate();
                    fetchPatients();
                } else if (res.status === 403) {
                    await Swal.fire('세션 만료', '다시 로그인 해주세요.', 'warning');
                    location.href = 'sign-in.html';
                } else {
                    let msg = '환자 추가 실패';
                    try {
                        msg = (await res.json()).msg || msg;
                    } catch {
                    }
                    Swal.fire('실패', msg, 'error');
                }
            } catch {
                Swal.fire('오류', '서버 오류로 추가에 실패했습니다.', 'error');
            } finally {
                setLoading(addBtn, false);
            }
        });

        validate();
    })();

    // ---- 모달 ----
    let isEditMode = false;
    let currentPatient = null;

    window.openPatientModal = function openPatientModal(patient) {
        currentPatient = patient;

        // --- 새 모달 UI에 데이터 채우기 ---
        // 헤더
        text('modal-patient-name-title', patient?.name ?? '환자 정보');
        text('modal-patient-id-subtitle', `ID: ${patient?.id ?? '정보 없음'}`);

        // 기본 정보
        text('modal-patient-name', patient?.name ?? '미등록');
        text('modal-patient-email', patient?.email ?? '정보 없음');
        text('modal-patient-managers', (patient?.managerIds ?? []).join(', ') || '없음');

        // 의료 정보 (읽기 모드)
        text('modal-patient-history', patient?.medicalHistory || '기록 없음');
        text('modal-patient-medications', (patient?.medications ?? []).join(', ') || '해당 없음');
        text('modal-patient-allergies', (patient?.allergies ?? []).join(', ') || '해당 없음');

        // 의료 정보 (편집 모드용 입력 필드)
        const historyInput = qid('modal-patient-history-input');
        if (historyInput) historyInput.value = patient?.medicalHistory ?? '';

        const medicationsInput = qid('modal-patient-medications-input');
        if (medicationsInput) medicationsInput.value = (patient?.medications ?? []).join(', ');

        const allergiesInput = qid('modal-patient-allergies-input');
        if (allergiesInput) allergiesInput.value = (patient?.allergies ?? []).join(', ');
        // --- 데이터 채우기 끝 ---

        setEditMode(false);
        qid('patient-modal')?.classList.remove('d-none');

        const editBtn = qid('modal-edit-btn');
        const isManager = (window.userRole === 'ROLE_USER_MANAGER' || window.userRole === 'ROLE_MANAGER');
        if (editBtn) {
            editBtn.disabled = !isManager;
            editBtn.textContent = isManager ? '수정하기' : '수정 권한 없음';
            editBtn.classList.toggle('bg-secondary', !isManager);
            editBtn.classList.toggle('text-secondary-emphasis', !isManager);
            editBtn.classList.toggle('bg-light', isManager);
            editBtn.classList.toggle('text-dark', isManager);
        }
    };

    function setEditMode(on) {
        isEditMode = on;
        qid('modal-patient-history')?.classList.toggle('d-none', on);
        qid('modal-patient-history-input')?.classList.toggle('d-none', !on);
        qid('modal-patient-medications')?.classList.toggle('d-none', on);
        qid('modal-patient-medications-input')?.classList.toggle('d-none', !on);
        qid('modal-patient-allergies')?.classList.toggle('d-none', on);
        qid('modal-patient-allergies-input')?.classList.toggle('d-none', !on);
        qid('modal-footer-btns')?.classList.toggle('d-none', on);
        qid('modal-footer-edit')?.classList.toggle('d-none', !on);
        if (on) qid('modal-patient-history-input')?.focus();
    }

    // 안전 바인딩
    on('modal-close-btn', 'click', () => qid('patient-modal')?.classList.add('d-none'), {assign: true});
    on('modal-edit-btn', 'click', () => setEditMode(true));
    on('modal-cancel-btn', 'click', () => setEditMode(false));
    on('modal-save-btn', 'click', async () => {
        const newHistory = qid('modal-patient-history-input')?.value.trim() ?? '';
        const newMedications = (qid('modal-patient-medications-input')?.value || '')
            .split(',').map(s => s.trim()).filter(Boolean);
        const newAllergies = (qid('modal-patient-allergies-input')?.value || '')
            .split(',').map(s => s.trim()).filter(Boolean);

        try {
            const res = await fetch(API_BASE + '/patient/update', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                credentials: 'include',
                body: JSON.stringify({
                    id: currentPatient?.id,
                    medicalHistory: newHistory,
                    medications: newMedications,
                    allergies: newAllergies
                })
            });
            if (res.ok) {
                text('modal-patient-history', newHistory);
                text('modal-patient-medications', newMedications.join(', '));
                text('modal-patient-allergies', newAllergies.join(', '));
                setEditMode(false);
                fetchPatients();
                alert('수정이 완료되었습니다.');
            } else {
                alert('수정 실패');
            }
        } catch {
            alert('서버 오류로 수정 실패');
        }
    });

    on('modal-delete-btn', 'click', async () => {
        if (!currentPatient?.id) {
            alert('환자 정보가 올바르지 않습니다.');
            return;
        }
        if (!confirm(`정말로 환자(${currentPatient.name})와의 연결을 해제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`)) return;

        try {
            const res = await fetch(API_BASE + '/patient/unlink-manager', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                credentials: 'include',
                body: JSON.stringify({patientId: currentPatient.id})
            });
            if (res.ok) {
                alert('환자와의 연결이 성공적으로 해제되었습니다.');
                qid('patient-modal')?.classList.add('d-none');
                fetchPatients();
            } else {
                const err = await res.json().catch(() => ({}));
                alert(`연결 해제 실패: ${err.message || '알 수 없는 오류'}`);
            }
        } catch (e) {
            console.error('unlink error', e);
            alert('서버 오류로 연결 해제에 실패했습니다.');
        }
    });
});