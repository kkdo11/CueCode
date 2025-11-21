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

// --- [3] DOMContentLoaded 메인 로직 ---
window.addEventListener('DOMContentLoaded', async () => {
    const patientListBody = document.getElementById('patient-list-tbody');
    let managedPatientIds = new Set(); // 관리하는 환자 ID 목록
    let statusPollingInterval = null; // 환자 상태 폴링 타이머

    // ---- 유틸 ----
    const qs = (sel) => document.querySelector(sel);
    const qid = (id) => document.getElementById(id);
    const show = (el, on = true, cls = 'd-none') => el && el.classList.toggle(cls, !on);
    const text = (id, v) => {
        const el = qid(id);
        if (el) el.textContent = v ?? '';
    };

    function on(id, evt, handler, {assign = false} = {}) {
        const el = qid(id);
        if (!el) {
            console.warn(`[bind] not found: #${id}`);
            return;
        }
        if (assign && evt === 'click') el.onclick = handler;
        else el.addEventListener(evt, handler);
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

    // ---- 환자 목록 조회 ----
    async function fetchPatients() {
        try {
            const managerId = await getManagerIdFromAPI();
            if (!managerId) {
                patientListBody.innerHTML = `<tr><td colspan="3" class="text-center text-danger">관리자 정보 없음</td></tr>`;
                return;
            }

            const res = await fetch(API_BASE + `/patient/list?managerId=${encodeURIComponent(managerId)}`, {credentials: 'include'});
            if (!res.ok) throw new Error('list failed');

            const arr = await res.json();
            patientListBody.innerHTML = '';
            managedPatientIds.clear();

            if (!Array.isArray(arr) || arr.length === 0) {
                patientListBody.innerHTML = `<tr><td colspan="3" class="text-center text-secondary">표시할 환자가 없습니다.</td></tr>`;
                // If there are no patients, stop any existing polling
                if (statusPollingInterval) {
                    clearInterval(statusPollingInterval);
                    statusPollingInterval = null;
                }
                return;
            }

            arr.forEach(p => {
                if (!p?.id) return;
                managedPatientIds.add(p.id);
                const tr = document.createElement('tr');
                // Set data attribute so other scripts can target this row by patient id
                tr.setAttribute('data-patient-id', p.id);
                tr.innerHTML = `
                    <td class="whitespace-nowrap">${p.id}</td>
                    <td class="whitespace-nowrap">${p.name ?? ''}</td>
                    <td class="whitespace-nowrap" id="status-${p.id}"><span class="spinner-border spinner-border-sm"></span></td>
                `;
                tr.addEventListener('click', async () => {
                    try {
                        const d = await fetch(API_BASE + `/patient/detail?id=${encodeURIComponent(p.id)}`, {credentials: 'include'});
                        if (d.ok) openPatientModal(await d.json());
                        else alert('환자 상세 정보를 불러오는 데 실패했습니다.');
                    } catch (e) {
                        console.error(e);
                        alert('환자 상세 정보를 요청하는 중 오류가 발생했습니다.');
                    }
                });
                patientListBody.appendChild(tr);
                fetchAndSetPatientStatus(p.id, tr);
            });

            // Clear any existing polling interval to avoid duplicates
            if (statusPollingInterval) {
                clearInterval(statusPollingInterval);
            }

            // Start polling for status updates every second
            statusPollingInterval = setInterval(() => {
                console.log('[DEBUG] Polling for patient status updates...');
                const rows = patientListBody.querySelectorAll('tr[data-patient-id]');
                rows.forEach(row => {
                    const patientId = row.getAttribute('data-patient-id');
                    if (patientId) {
                        // Fire and forget, don't await, to poll all patients in parallel
                        fetchAndSetPatientStatus(patientId, row);
                    }
                });
            }, 1000); // Poll every 1 second

        } catch (e) {
            console.error('fetchPatients error:', e);
            patientListBody.innerHTML = `<tr><td colspan="3" class="text-center text-secondary">환자 목록을 불러오지 못했습니다.</td></tr>`;
        }
    }

    // ---- 환자 상태 조회 (마지막 구문) ----
    async function fetchAndSetPatientStatus(patientId, tr) {
        const statusCell = tr.querySelector(`#status-${patientId}`);
        if (!statusCell) return;
        try {
            const response = await fetch(`${API_BASE}/motions/history/last?patientId=${patientId}`, {credentials: 'include'});
            if (response.ok) {
                const data = await response.json();
                // Normalize phrase: ensure string, trim whitespace
                const phraseRaw = typeof data.phrase === 'string' ? data.phrase : (data.phrase ?? '기록 없음');
                const phrase = phraseRaw.trim();
                statusCell.textContent = phrase;
                console.log(`[DEBUG] Patient ${patientId}: Fetched phrase: "${phrase}"`);

                // Use includes to be robust against extra context or punctuation
                const isDanger = (phrase.includes('도와주세요') || phrase.includes('아파요'));
                if (isDanger) {
                    // Previously we highlighted the table row in red. Instead, trigger the
                    // top-right SweetAlert2 toast via window.processDetectedAlerts.
                    // Construct a minimal alert object compatible with processDetectedAlerts.
                    const nameCell = tr.querySelector('td:nth-child(2)');
                    const patientName = nameCell ? (nameCell.textContent || '').trim() : '';
                    try {
                        window.processDetectedAlerts([{ userId: patientId, userName: patientName, phrase: phrase, confirmed: false }]);
                        console.log(`[DEBUG] Patient ${patientId}: processDetectedAlerts invoked for danger phrase.`);
                    } catch (e) {
                        console.error('[ERROR] calling processDetectedAlerts:', e);
                    }
                } else {
                    tr.classList.remove('alert-red-row'); // Ensure it's removed if phrase changes
                    try { tr.style.backgroundColor = ''; tr.style.color = ''; } catch(e) {}
                    tr.querySelectorAll('td').forEach(td => {
                        td.style.backgroundColor = '';
                        td.style.color = '';
                    });
                    console.log(`[DEBUG] Patient ${patientId}: Not dangerous. Removed 'alert-red-row' class if present.`);
                }
            } else {
                statusCell.textContent = '정보 없음';
                tr.classList.remove('alert-red-row'); // Ensure it's removed on error
                console.log(`[DEBUG] Patient ${patientId}: Failed to fetch status or response not OK. Removed 'alert-red-row' class.`);
            }
        } catch (error) {
            console.error(`Error fetching status for patient ${patientId}:`, error);
            statusCell.textContent = '오류';
        }
    }

    // ---- 응급 알러트 (웹소켓) ----
    let currentAlertId = null;
    const alertModalEl = document.getElementById('emergency-alert-modal');
    const alertModal = new bootstrap.Modal(alertModalEl);

    function connectWebSocket() {
        const wsUrl = API_BASE.replace(/^http/, 'ws') + '/ws/alerts';
        console.log('Connecting to WebSocket:', wsUrl);
        const socket = new WebSocket(wsUrl);

        socket.onmessage = (event) => {
            console.log("[WebSocket] Raw message received:", event.data);
            try {
                const alert = JSON.parse(event.data);
                console.log("[WebSocket] Parsed alert object:", alert);
                console.log("[WebSocket] Current managed patient IDs:", Array.from(managedPatientIds));

                if (alert && alert.userId) {
                    const isManaged = managedPatientIds.has(alert.userId);
                    console.log(`[WebSocket] Checking if patient ${alert.userId} is managed: ${isManaged}`);

                    // 내가 관리하는 환자의 알림인지 확인
                    if (isManaged) {
                        console.log("[WebSocket] Alert is for a managed patient. Processing with window.processDetectedAlerts.");
                        // Call the global function defined in dashboard.html
                        // Pass the alert object as an array, as processDetectedAlerts expects an array
                        // Only trigger for specific dangerous phrases
                        // Use includes() with normalization to be robust to extra whitespace/punctuation
                        const phraseNormalized = String(alert.phrase ?? '').trim();
                        if (phraseNormalized.includes('도와주세요') || phraseNormalized.includes('아파요')) {
                            console.log("[WebSocket] Dangerous phrase detected for managed patient:", phraseNormalized);
                            window.processDetectedAlerts([alert]);
                        } else {
                            console.log("[WebSocket] Alert is for a managed patient but phrase not considered dangerous:", phraseNormalized);
                        }
                        // Keep the modal logic if it's for a different type of alert or for future use
                        // document.getElementById('alert-patient-id').textContent = alert.userId;
                        // document.getElementById('alert-patient-name').textContent = alert.userName || '(이름 정보 없음)';
                        // currentAlertId = alert.id; // alert.id가 alertId라고 가정
                        // alertModal.show(); // Do not show the modal for the top-right alert
                    } else {
                        console.log("[WebSocket] Alert is for a patient not managed by the current user. Ignoring.");
                    }
                } else {
                    console.warn("[WebSocket] Received alert object is invalid or has no userId.", alert);
                }
            } catch (e) {
                console.error('Error processing alert message:', e);
            }
        };

        socket.onclose = (event) => {
            console.log('WebSocket closed. Reconnecting in 5 seconds...', event.reason);
            setTimeout(connectWebSocket, 5000);
        };

        socket.onerror = (error) => {
            console.error('WebSocket Error:', error);
            socket.close();
        };
    }

    on('confirm-alert-btn', 'click', async () => {
        if (currentAlertId) {
            try {
                await fetch(API_BASE + `/motions/alerts/confirm/${currentAlertId}`, {
                    method: 'POST',
                    credentials: 'include'
                });
                currentAlertId = null;
                alertModal.hide();
            } catch (e) {
                console.error('Failed to confirm alert:', e);
                alert('알러트 확인 처리에 실패했습니다.');
            }
        }
    });

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
                const res = await fetch(API_BASE + `/patient/detail?id=${encodeURIComponent(id)}`, {credentials: 'include'});
                if (res.ok) {
                    const data = await res.json();
                    resultIcon?.setAttribute('icon', 'solar:shield-check-linear');
                    resultTitle.textContent = `${data.name ?? '이름 미등록'} (ID: ${data.id ?? id})`;
                    resultDesc.textContent = `담당 매니저: ${(data.managerIds ?? []).join(', ') || '없음'}`;
                    show(resultBox, true);
                } else if (res.status === 404) {
                    resultIcon?.setAttribute('icon', 'solar:danger-triangle-linear');
                    resultTitle.textContent = '등록되지 않은 환자입니다.';
                    resultDesc.textContent = 'ID를 확인 후 추가를 진행해주세요.';
                    show(resultBox, true);
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
                    await Swal.fire({icon: 'success', title: '추가 완료', text: `${patientId} 환자가 연결되었습니다.`, timer: 1400, showConfirmButton: false});
                    if (idInput) idInput.value = '';
                    validate();
                    fetchPatients();
                } else {
                    let msg = '환자 추가 실패';
                    try { msg = (await res.json()).msg || msg; } catch {}
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
        text('modal-patient-name-title', patient?.name ?? '환자 정보');
        text('modal-patient-id-subtitle', `ID: ${patient?.id ?? '정보 없음'}`);
        text('modal-patient-name', patient?.name ?? '미등록');
        text('modal-patient-email', patient?.email ?? '정보 없음');
        text('modal-patient-managers', (patient?.managerIds ?? []).join(', ') || '없음');
        text('modal-patient-history', patient?.medicalHistory || '기록 없음');
        text('modal-patient-medications', (patient?.medications ?? []).join(', ') || '해당 없음');
        text('modal-patient-allergies', (patient?.allergies ?? []).join(', ') || '해당 없음');

        const historyInput = qid('modal-patient-history-input');
        if (historyInput) historyInput.value = patient?.medicalHistory ?? '';
        const medicationsInput = qid('modal-patient-medications-input');
        if (medicationsInput) medicationsInput.value = (patient?.medications ?? []).join(', ');
        const allergiesInput = qid('modal-patient-allergies-input');
        if (allergiesInput) allergiesInput.value = (patient?.allergies ?? []).join(', ');

        setEditMode(false);
        qid('patient-modal')?.classList.remove('d-none');

        // 디버깅: window.userRole 값 확인
        console.log('[DEBUG] openPatientModal - window.userRole:', window.userRole);
        console.log('[DEBUG] openPatientModal - typeof window.userRole:', typeof window.userRole);

        const editBtn = qid('modal-edit-btn');
        // ROLE_ADMIN 권한도 포함하도록 수정
        const isManager = ['ROLE_USER_MANAGER', 'ROLE_MANAGER', 'ROLE_ADMIN'].includes(window.userRole);
        console.log('[DEBUG] openPatientModal - isManager:', isManager);

        if (editBtn) {
            editBtn.disabled = !isManager;
            editBtn.textContent = isManager ? '수정하기' : '수정 권한 없음';
            console.log('[DEBUG] openPatientModal - editBtn.disabled:', editBtn.disabled, ', editBtn.textContent:', editBtn.textContent);
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

    on('modal-close-btn', 'click', () => qid('patient-modal')?.classList.add('d-none'), {assign: true});
    on('modal-edit-btn', 'click', () => setEditMode(true));
    on('modal-cancel-btn', 'click', () => setEditMode(false));
    on('modal-save-btn', 'click', async () => {
        const newHistory = qid('modal-patient-history-input')?.value.trim() ?? '';
        const newMedications = (qid('modal-patient-medications-input')?.value || '').split(',').map(s => s.trim()).filter(Boolean);
        const newAllergies = (qid('modal-patient-allergies-input')?.value || '').split(',').map(s => s.trim()).filter(Boolean);

        try {
            const res = await fetch(API_BASE + '/patient/update', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                credentials: 'include',
                body: JSON.stringify({id: currentPatient?.id, medicalHistory: newHistory, medications: newMedications, allergies: newAllergies})
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

    // ---- 초기화 ----
    await fetchPatients();
    connectWebSocket();
});
