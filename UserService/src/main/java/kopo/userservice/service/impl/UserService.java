package kopo.userservice.service.impl;

import kopo.userservice.auth.JwtTokenProvider;
import kopo.userservice.dto.PatientDTO;
import kopo.userservice.dto.ManagerDTO;
import kopo.userservice.dto.MailDTO;
import kopo.userservice.dto.MsgDTO;
import kopo.userservice.repository.PatientRepository;
import kopo.userservice.repository.ManagerRepository;
import kopo.userservice.repository.DetectionAreaRepository;
import kopo.userservice.model.PatientDocument;
import kopo.userservice.model.ManagerDocument;
import kopo.userservice.repository.document.DetectionAreaDocument;
import kopo.userservice.service.IUserService;
import kopo.userservice.service.IMailService;
import kopo.userservice.util.CmmUtil;
import kopo.userservice.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import kopo.userservice.auth.JwtTokenType;
import jakarta.servlet.http.HttpServletRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService implements IUserService {
    private final PatientRepository patientRepository;
    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;
    private final DetectionAreaRepository detectionAreaRepository;
    private final IMailService mailService;
    private final RedisUtil redisUtil;
    private final JwtTokenProvider jwtTokenProvider;


    @Override
    public Object login(String userId, String password) {
        log.info("[login] 로그인 시도: userId={}, password={}", userId, password != null ? "***" : null);
        if (userId == null || userId.isBlank()) {
            log.warn("[login] userId가 null 또는 빈 문자열입니다.");
            return null;
        }
        // 1. 환자 인증 시도
        Optional<PatientDocument> patientOpt = patientRepository.findById(userId);
        if (patientOpt.isPresent()) {
            log.info("[login] 환자(userId={}) 정보 조회 성공", userId);
            PatientDocument patient = patientOpt.get();
            if (patient.getPw() != null && passwordEncoder.matches(password, patient.getPw())) {
                log.info("[login] 환자(userId={}) 비밀번호 일치, 인증 성공", userId);
                return PatientDTO.builder()
                        .id(patient.getId())
                        .pw(patient.getPw())
                        .email(patient.getEmail())
                        .name(patient.getName())
                        .managerIds(patient.getManagerIds())
                        .build();
            } else {
                log.info("[login] 환자(userId={}) 비밀번호 불일치", userId);
            }
        } else {
            log.info("[login] 환자(userId={}) 정보 없음", userId);
        }
        // 2. 관리자 인증 시도
        Optional<ManagerDocument> managerOpt = managerRepository.findById(userId);
        if (managerOpt.isPresent()) {
            log.info("[login] 관리자(userId={}) 정보 조회 성공", userId);
            ManagerDocument manager = managerOpt.get();
            if (manager.getPw() != null && passwordEncoder.matches(password, manager.getPw())) {
                log.info("[login] 관리자(userId={}) 비밀번호 일치, 인증 성공", userId);
                return ManagerDTO.builder()
                        .id(manager.getId())
                        .pw(manager.getPw())
                        .email(manager.getEmail())
                        .name(manager.getName())
                        .patientIds(manager.getPatientIds())
                        .build();
            } else {
                log.info("[login] 관리자(userId={}) 비밀번호 불일치", userId);
            }
        } else {
            log.info("[login] 관리자(userId={}) 정보 없음", userId);
        }
        log.info("[login] 인증 실패: userId={}", userId);
        // 인증 실패 시 null 반환
        return null;
    }

    @Override
    public int insertPatient(PatientDTO dto) {
        log.info("insertPatient Start! 입력값: {}", dto);
        String id = CmmUtil.nvl(dto.id());
        String pw = CmmUtil.nvl(dto.pw());
        String email = CmmUtil.nvl(dto.email());
        String name = CmmUtil.nvl(dto.name());
        String detectionAreaType = CmmUtil.nvl(dto.detectionAreaType());
        log.info("id : {}", id);
        log.info("pw : {}", pw);
        log.info("email : {}", email);
        log.info("name : {}", name);
        log.info("detectionAreaType : {}", detectionAreaType);
        Optional<PatientDocument> rEntity = patientRepository.findById(id);
        if (rEntity.isPresent()) {
            log.info("중복 아이디: {}", id);
            return 2;
        }
        PatientDocument entity = PatientDocument.builder()
                .id(id)
                .pw(pw)
                .email(email)
                .name(name)
                .managerIds(dto.managerIds())
                .build();
        patientRepository.save(entity);
        log.info("insertPatient 저장 결과: 성공");
        // detection_area 저장 로직 개선
        boolean hand = false, face = false, both = false;
        if ("hand".equalsIgnoreCase(detectionAreaType)) hand = true;
        else if ("face".equalsIgnoreCase(detectionAreaType)) face = true;
        else if ("both".equalsIgnoreCase(detectionAreaType)) both = true;
        Optional<DetectionAreaDocument> existing = detectionAreaRepository.findByPatientId(id);
        DetectionAreaDocument detectionArea;
        if (existing.isPresent()) {
            detectionArea = existing.get();
            detectionArea.setHand(hand);
            detectionArea.setFace(face);
            detectionArea.setBoth(both);
        } else {
            detectionArea = DetectionAreaDocument.builder()
                .patientId(id)
                .hand(hand)
                .face(face)
                .both(both)
                .build();
        }
        detectionAreaRepository.save(detectionArea);
        log.info("detection_area 저장 결과: patientId={}, hand={}, face={}, both={}", id, hand, face, both);
        return 1;
    }

    @Override
    public int insertManager(ManagerDTO dto) {
        log.info("insertManager Start! 입력값: {}", dto);
        String id = CmmUtil.nvl(dto.id());
        String pw = CmmUtil.nvl(dto.pw());
        String email = CmmUtil.nvl(dto.email());
        String name = CmmUtil.nvl(dto.name());
        log.info("id : {}", id);
        log.info("pw : {}", pw);
        log.info("email : {}", email);
        log.info("name : {}", name);
        Optional<ManagerDocument> rEntity = managerRepository.findById(id);
        if (rEntity.isPresent()) {
            log.info("중복 아이디: {}", id);
            return 2;
        }
        ManagerDocument entity = ManagerDocument.builder()
                .id(id)
                .pw(pw)
                .email(email)
                .name(name)
                .patientIds(dto.patientIds())
                .build();
        managerRepository.save(entity);
        boolean saved = managerRepository.findById(id).isPresent();
        log.info("insertManager 저장 결과: {}", saved ? "성공" : "실패");
        return saved ? 1 : 0;
    }

    @Override
    public PatientDTO getPatient(PatientDTO dto) {
        log.info("getPatient Start! 조회 아이디: {}", dto.id());
        String id = CmmUtil.nvl(dto.id());
        Optional<PatientDocument> entity = patientRepository.findById(id);
        if (entity.isPresent()) {
            PatientDocument e = entity.get();
            log.info("getPatient 조회 성공: {}", e);
            return PatientDTO.builder()
                    .id(e.getId())
                    .pw(e.getPw())
                    .email(e.getEmail())
                    .name(e.getName())
                    .managerIds(e.getManagerIds())
                    .build();
        }
        log.info("getPatient 조회 실패: 아이디 {} 없음", id);
        return null;
    }

    @Override
    public ManagerDTO getManager(ManagerDTO dto) {
        log.info("getManager Start! 조회 아이디: {}", dto.id());
        String id = CmmUtil.nvl(dto.id());
        Optional<ManagerDocument> entity = managerRepository.findById(id);
        if (entity.isPresent()) {
            ManagerDocument e = entity.get();
            log.info("getManager 조회 성공: {}", e);
            return new ManagerDTO(e.getId(), e.getPw(), e.getEmail(), e.getName(), e.getPatientIds());
        }
        log.info("getManager 조회 실패: 아이디 {} 없음", dto.id());
        return null;
    }

    @Override
    public boolean existsUserId(String userId) {
        if (userId == null || userId.isBlank()) return false;
        boolean existsPatient = patientRepository.findById(userId).isPresent();
        boolean existsManager = managerRepository.findById(userId).isPresent();
        return existsPatient || existsManager;
    }

    /**
     * 인증번호 포함 HTML 이메일 발송 (이메일 인증)
     * @param email 수신자 이메일
     * @return 발송된 인증번호
     */
    public int sendEmailAuthCode(String email) {
        log.info("sendEmailAuthCode Start!");
        if (email == null || email.trim().isEmpty()) {
            log.warn("[sendEmailAuthCode] 이메일 파라미터가 비어 있습니다.");
            throw new IllegalArgumentException("이메일 주소가 입력되지 않았습니다.");
        }

        // 원본 이메일 로깅
        log.info("[sendEmailAuthCode] 원본 이메일: {}", email);

        // 이메일 정규화 및 로깅
        String normalizedEmail = email.trim().toLowerCase();
        log.info("[sendEmailAuthCode] 정규화된 이메일: {}", normalizedEmail);

        int authCode = ThreadLocalRandom.current().nextInt(100000, 1000000);
        String key = "emailAuth:" + normalizedEmail;

        // Redis 저장 전 로깅
        log.info("[sendEmailAuthCode] Redis 저장 시도 key: {}, value: {}", key, authCode);

        // Redis 저장 후 실제 저장 여부 확인
        redisUtil.set(key, String.valueOf(authCode), 180); // 3분 유효
        String savedValue = redisUtil.get(key);
        log.info("[sendEmailAuthCode] Redis 저장 확인 key: {}, 저장된 value: {}", key, savedValue);

        // 이메일 발송 로직 (이전과 동일하게 MailService 사용)
        MailDTO dto = new MailDTO();
        dto.setTitle("[CueCode] 이메일 인증번호");
        String contents = "<div style='max-width:600px; margin:0 auto; padding:40px 30px; font-family:Arial, sans-serif; border:1px solid #e0e0e0; border-radius:10px; box-shadow:0 2px 8px rgba(0,0,0,0.1);'>";
        contents += "    <div style='text-align:center;'>";
        contents += "        <h2 style='color:#1976d2; margin-bottom:10px;'>CueCode 이메일 인증</h2>";
        contents += "        <p style='font-size:16px; color:#333; margin-bottom:30px;'>아래 인증번호를 입력하여 이메일 인증을 완료해주세요.</p>";
        contents += "        <div style='font-size:28px; font-weight:bold; margin:20px 0; color:#1976d2; letter-spacing:4px;'>" + authCode + "</div>";
        contents += "        <p style='font-size:12px; color:#aaa; margin-top:40px;'>본 메일은 발신전용입니다. 문의사항은 홈페이지를 통해 접수해주세요.<br>© CueCode Team</p>";
        contents += "    </div></div>";
        dto.setContents(contents);
        dto.setToMail(email);
        mailService.doSendMail(dto);
        log.info("sendEmailAuthCode End!");
        return authCode;
    }

    @Override
    public String findUserIdByEmail(String email) {
        log.info("[findUserIdByEmail] 이메일로 사용자 ID 찾기 시도: {}", email);
        if (email == null || email.isBlank()) {
            log.warn("[findUserIdByEmail] 이메일 파라미터가 비어 있습니다.");
            return null;
        }

        String encryptedEmail;
        try {
            encryptedEmail = kopo.userservice.util.EncryptUtil.encAES128CBC(email);
            log.info("[findUserIdByEmail] 암호화된 이메일: {}", encryptedEmail);
        } catch (Exception e) {
            log.error("[findUserIdByEmail] 이메일 암호화 실패", e);
            return null;
        }

        // 1. 환자(Patient)에서 검색
        Optional<PatientDocument> patientOpt = patientRepository.findByEmail(encryptedEmail);
        if (patientOpt.isPresent()) {
            String userId = patientOpt.get().getId();
            log.info("[findUserIdByEmail] 환자 ID 찾기 성공: {}", userId);
            return userId;
        }

        // 2. 관리자(Manager)에서 검색
        Optional<ManagerDocument> managerOpt = managerRepository.findByEmail(encryptedEmail);
        if (managerOpt.isPresent()) {
            String userId = managerOpt.get().getId();
            log.info("[findUserIdByEmail] 관리자 ID 찾기 성공: {}", userId);
            return userId;
        }

        log.info("[findUserIdByEmail] 이메일 {}에 해당하는 사용자 ID를 찾을 수 없습니다.", email);
        return null;
    }

    /**
     * 로그아웃 시 Access Token과 Refresh Token을 블랙리스트에 등록합니다.
     */
    @Override
    public void invalidateRefreshToken(HttpServletRequest request) {
        log.info("[invalidateRefreshToken] 토큰 삭제 시도");

        // Access Token 추출 및 삭제
        String accessToken = jwtTokenProvider.resolveToken(request, JwtTokenType.ACCESS_TOKEN);
        log.info("[invalidateRefreshToken] accessToken: {}", accessToken);
        if (accessToken != null) {
            redisUtil.delete("access:" + jwtTokenProvider.getUserIdFromToken(accessToken));
            log.info("[invalidateRefreshToken] Access Token 삭제 완료.");
        } else {
            log.warn("[invalidateRefreshToken] Access Token 추출 실패.");
        }

        // Refresh Token 추출 및 삭제
        String refreshToken = jwtTokenProvider.resolveToken(request, JwtTokenType.REFRESH_TOKEN);
        log.info("[invalidateRefreshToken] refreshToken: {}", refreshToken);
        if (refreshToken != null) {
            redisUtil.delete("refresh:" + jwtTokenProvider.getUserIdFromToken(refreshToken));
            log.info("[invalidateRefreshToken] Refresh Token 삭제 완료.");
        } else {
            log.warn("[invalidateRefreshToken] Refresh Token 추출 실패.");
        }
    }


    @Override
    public boolean verifyPassword(String userId, String password) {
        // 환자 인증
        Optional<PatientDocument> patientOpt = patientRepository.findById(userId);
        if (patientOpt.isPresent()) {
            PatientDocument patient = patientOpt.get();
            return patient.getPw() != null && passwordEncoder.matches(password, patient.getPw());
        }
        // 관리자 인증
        Optional<ManagerDocument> managerOpt = managerRepository.findById(userId);
        if (managerOpt.isPresent()) {
            ManagerDocument manager = managerOpt.get();
            return manager.getPw() != null && passwordEncoder.matches(password, manager.getPw());
        }
        // 해당 userId가 없으면 false
        return false;
    }

    @Override
    public kopo.userservice.dto.UserInfoDTO getUserInfo(String userId) {
        log.info("getUserInfo 호출: userId={}", userId);
        if (userId == null || userId.isBlank()) {
            log.warn("getUserInfo: userId가 null 또는 빈 문자열입니다.");
            return null;
        }
        Optional<PatientDocument> patientOpt = patientRepository.findById(userId);
        if (patientOpt.isPresent()) {
            PatientDocument patient = patientOpt.get();
            String decryptedEmail = null;
            try {
                decryptedEmail = kopo.userservice.util.EncryptUtil.decAES128CBC(patient.getEmail());
            } catch (Exception e) {
                log.error("환자 이메일 복호화 실패: userId={}", userId, e);
                // 복호화 실패 시 원본 또는 마스킹된 값 반환 고려
                decryptedEmail = "[복호화 오류]"; // 또는 patient.getEmail();
            }
            // 환자의 경우 managerId는 null 또는 빈 값이어야 함
            return kopo.userservice.dto.UserInfoDTO.builder()
                    .id(patient.getId())
                    .email(decryptedEmail)
                    .name(patient.getName())
                    .userType("patient")
                    .managerId(null) // 환자는 managerId가 없음
                    .build();
        }

        Optional<ManagerDocument> managerOpt = managerRepository.findById(userId);
        if (managerOpt.isPresent()) {
            ManagerDocument manager = managerOpt.get();
            String decryptedEmail = null;
            try {
                decryptedEmail = kopo.userservice.util.EncryptUtil.decAES128CBC(manager.getEmail());
            } catch (Exception e) {
                log.error("관리자 이메일 복호화 실패: userId={}", userId, e);
                decryptedEmail = "[복호화 오류]"; // 또는 manager.getEmail();
            }

            // 💡 [수정] managerId를 DTO 빌더에 추가
            return kopo.userservice.dto.UserInfoDTO.builder()
                    .id(manager.getId())
                    .email(decryptedEmail)
                    .name(manager.getName())
                    .userType("manager")
                    .managerId(manager.getManagerId()) // managerId 필드 추가 (ManagerDocument의 실제 필드명 확인 필요)
                    .build();
        }

        log.info("getUserInfo: userId={}에 해당하는 사용자 없음", userId);
        return null;
    }

    // ... (다른 메서드)


    @Override
    public boolean updateName(String userId, String newName) {
        if (userId == null || userId.isBlank() || newName == null || newName.isBlank()) {
            log.warn("updateName: userId 또는 newName이 비어 있습니다.");
            return false;
        }
        Optional<PatientDocument> patientOpt = patientRepository.findById(userId);
        if (patientOpt.isPresent()) {
            PatientDocument patient = patientOpt.get();
            patient.setName(newName);
            patientRepository.save(patient);
            log.info("updateName: 환자 이름 변경 성공 userId={}, newName={}", userId, newName);
            return true;
        }
        Optional<ManagerDocument> managerOpt = managerRepository.findById(userId);
        if (managerOpt.isPresent()) {
            ManagerDocument manager = managerOpt.get();
            manager.setName(newName);
            managerRepository.save(manager);
            log.info("updateName: 관리자 이름 변경 성공 userId={}, newName={}", userId, newName);
            return true;
        }
        log.warn("updateName: userId={}에 해당하는 사용자 없음", userId);
        return false;
    }

    @Override
    public boolean updateEmail(String userId, String newEmail) {
        if (userId == null || userId.isBlank() || newEmail == null || newEmail.isBlank()) {
            log.warn("updateEmail: userId 또는 newEmail이 비어 있습니다.");
            return false;
        }
        Optional<PatientDocument> patientOpt = patientRepository.findById(userId);
        if (patientOpt.isPresent()) {
            PatientDocument patient = patientOpt.get();
            patient.setEmail(newEmail);
            patientRepository.save(patient);
            log.info("updateEmail: 환자 이메일 변경 성공 userId={}, newEmail={}", userId, newEmail);
            return true;
        }
        Optional<ManagerDocument> managerOpt = managerRepository.findById(userId);
        if (managerOpt.isPresent()) {
            ManagerDocument manager = managerOpt.get();
            manager.setEmail(newEmail);
            managerRepository.save(manager);
            log.info("updateEmail: 관리자 이메일 변경 성공 userId={}, newEmail={}", userId, newEmail);
            return true;
        }
        log.warn("updateEmail: userId={}에 해당하는 사용자 없음", userId);
        return false;
    }

    @Override
    public boolean updateId(String userId, String newId) {
        if (userId == null || userId.isBlank() || newId == null || newId.isBlank()) {
            log.warn("updateId: userId 또는 newId가 비어 있습니다.");
            return false;
        }
        // 이미 newId가 존재하는지 확인
        if (patientRepository.findById(newId).isPresent() || managerRepository.findById(newId).isPresent()) {
            log.warn("updateId: newId={} 이미 존재함", newId);
            return false;
        }
        Optional<PatientDocument> patientOpt = patientRepository.findById(userId);
        if (patientOpt.isPresent()) {
            PatientDocument patient = patientOpt.get();
            patientRepository.deleteById(userId);
            patient.setId(newId);
            patientRepository.save(patient);
            log.info("updateId: 환자 아이디 변경 성공 userId={}, newId={}", userId, newId);
            return true;
        }
        Optional<ManagerDocument> managerOpt = managerRepository.findById(userId);
        if (managerOpt.isPresent()) {
            ManagerDocument manager = managerOpt.get();
            managerRepository.deleteById(userId);
            manager.setId(newId);
            managerRepository.save(manager);
            log.info("updateId: 관리자 아이디 변경 성공 userId={}, newId={}", userId, newId);
            return true;
        }
        log.warn("updateId: userId={}에 해당하는 사용자 없음", userId);
        return false;
    }

    @Override
    public boolean updatePassword(String userId, String currentPassword, String newPassword) {
        if (userId == null || userId.isBlank() || currentPassword == null || currentPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            log.warn("updatePassword: 파라미터가 비어 있습니다.");
            return false;
        }
        Optional<PatientDocument> patientOpt = patientRepository.findById(userId);
        if (patientOpt.isPresent()) {
            PatientDocument patient = patientOpt.get();
            if (patient.getPw() != null && passwordEncoder.matches(currentPassword, patient.getPw())) {
                patient.setPw(passwordEncoder.encode(newPassword));
                patientRepository.save(patient);
                log.info("updatePassword: 환자 비밀번호 변경 성공 userId={}", userId);
                return true;
            } else {
                log.warn("updatePassword: 환자 현재 비밀번호 불일치 userId={}", userId);
                return false;
            }
        }
        Optional<ManagerDocument> managerOpt = managerRepository.findById(userId);
        if (managerOpt.isPresent()) {
            ManagerDocument manager = managerOpt.get();
            if (manager.getPw() != null && passwordEncoder.matches(currentPassword, manager.getPw())) {
                manager.setPw(passwordEncoder.encode(newPassword));
                managerRepository.save(manager);
                log.info("updatePassword: 관리자 비밀번호 변경 성공 userId={}", userId);
                return true;
            } else {
                log.warn("updatePassword: 관리자 현재 비밀번호 불일치 userId={}", userId);
                return false;
            }
        }
        log.warn("updatePassword: userId={}에 해당하는 사용자 없음", userId);
        return false;
    }

    public MsgDTO withdrawalUser(String userId) {
        log.info("[UserService] 회원탈퇴 요청: {}", userId);
        try {
            Optional<PatientDocument> patientOpt = patientRepository.findById(userId);
            if (patientOpt.isPresent()) {
                patientRepository.deleteById(userId);
                return MsgDTO.builder().result(1).msg("환자 회원 탈퇴 성공").build();
            }
            Optional<ManagerDocument> managerOpt = managerRepository.findById(userId);
            if (managerOpt.isPresent()) {
                managerRepository.deleteById(userId);
                return MsgDTO.builder().result(1).msg("관리자 회원 탈퇴 성공").build();
            }
            return MsgDTO.builder().result(0).msg("해당 회원을 찾을 수 없습니다.").build();
        } catch (Exception e) {
            log.error("회원탈퇴 중 오류", e);
            return MsgDTO.builder().result(0).msg("회원탈퇴 처리 중 오류 발생").build();
        }
    }

    @Override
    public java.util.Map<String, Boolean> getDetectionArea(String userId) {
        log.info("[getDetectionArea] 감지 범위 조회 시작: userId={}", userId);
        Optional<DetectionAreaDocument> doc = detectionAreaRepository.findByPatientId(userId);
        if (doc.isPresent()) {
            DetectionAreaDocument area = doc.get();
            java.util.Map<String, Boolean> result = new java.util.HashMap<>();
            result.put("hand", area.isHand());
            result.put("face", area.isFace());
            result.put("both", area.isBoth());
            log.info("[getDetectionArea] 감지 범위 조회 성공: {}", result);
            return result;
        }
        log.warn("[getDetectionArea] 감지 범위 정보를 찾을 수 없음: userId={}", userId);
        return java.util.Collections.emptyMap();
    }

    @Override
    public boolean updateDetectionArea(String userId, String detectionAreaType) {
        log.info("[updateDetectionArea] 감지 범위 업데이트 시작: userId={}, type={}", userId, detectionAreaType);
        Optional<DetectionAreaDocument> existing = detectionAreaRepository.findByPatientId(userId);
        if (existing.isEmpty()) {
            log.warn("[updateDetectionArea] 업데이트할 감지 범위 문서가 없음: userId={}", userId);
            return false;
        }

        DetectionAreaDocument detectionArea = existing.get();
        boolean hand = "hand".equalsIgnoreCase(detectionAreaType) || "both".equalsIgnoreCase(detectionAreaType);
        boolean face = "face".equalsIgnoreCase(detectionAreaType) || "both".equalsIgnoreCase(detectionAreaType);
        boolean both = "both".equalsIgnoreCase(detectionAreaType);

        detectionArea.setHand(hand);
        detectionArea.setFace(face);
        detectionArea.setBoth(both);

        detectionAreaRepository.save(detectionArea);
        log.info("[updateDetectionArea] 감지 범위 업데이트 성공");
        return true;
    }
}
