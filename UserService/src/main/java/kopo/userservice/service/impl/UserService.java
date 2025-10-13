package kopo.userservice.service.impl;

import kopo.userservice.auth.JwtTokenProvider;
import kopo.userservice.dto.PatientDTO;
import kopo.userservice.dto.ManagerDTO;
import kopo.userservice.dto.MailDTO;
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
        // detection_area 저장 로직 추가
        boolean hand = false, face = false, both = false;
        if ("hand".equalsIgnoreCase(detectionAreaType)) hand = true;
        else if ("face".equalsIgnoreCase(detectionAreaType)) face = true;
        else if ("both".equalsIgnoreCase(detectionAreaType)) both = true;
        DetectionAreaDocument detectionArea = DetectionAreaDocument.builder()
                .patientId(id)
                .hand(hand)
                .face(face)
                .both(both)
                .build();
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
    /**
     * 로그아웃 시 Access Token과 Refresh Token을 블랙리스트에 등록합니다.
     */
    @Override
    public void invalidateRefreshToken(HttpServletRequest request) {
        log.info("[invalidateRefreshToken] 토큰 블랙리스트 등록 시도");

        // Access Token 추출
        String accessToken = jwtTokenProvider.resolveToken(request, JwtTokenType.ACCESS_TOKEN);
        if (accessToken != null) {
            try {
                long accessExp = getExpiration(accessToken);
                String accessKey = "blacklist:access:" + accessToken;
                redisUtil.set(accessKey, "logged_out", accessExp);
                log.info("[invalidateRefreshToken] Access Token 블랙리스트 등록 완료. TTL: {}초", accessExp);
            } catch (Exception e) {
                log.error("[invalidateRefreshToken] Access Token 블랙리스트 등록 오류: {}", e.getMessage());
            }
        } else {
            log.warn("[invalidateRefreshToken] Access Token 추출 실패.");
        }

        // Refresh Token 추출
        String refreshToken = jwtTokenProvider.resolveToken(request, JwtTokenType.REFRESH_TOKEN);
        if (refreshToken != null) {
            try {
                long refreshExp = getExpiration(refreshToken);
                String refreshKey = "blacklist:refresh:" + refreshToken;
                redisUtil.set(refreshKey, "logged_out", refreshExp);
                log.info("[invalidateRefreshToken] Refresh Token 블랙리스트 등록 완료. TTL: {}초", refreshExp);
            } catch (Exception e) {
                log.error("[invalidateRefreshToken] Refresh Token 블랙리스트 등록 오류: {}", e.getMessage());
            }
        } else {
            log.warn("[invalidateRefreshToken] Refresh Token 추출 실패.");
        }
    }

    /**
     * JWT에서 만료시간(초)을 추출하는 유틸 함수
     */
    private long getExpiration(String token) {
        Claims claims = Jwts.parserBuilder()
                .build()
                .parseClaimsJws(token)
                .getBody();
        long expMillis = claims.getExpiration().getTime();
        long nowMillis = System.currentTimeMillis();
        return (expMillis - nowMillis) / 1000;
    }
}
