package kopo.userservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashMap; // HashMap 사용을 위해 import
import java.util.Map;
import java.util.Collections;
import kopo.userservice.dto.UserLoginRequestDTO;
import kopo.userservice.dto.MsgDTO;
import kopo.userservice.dto.UserInfoDTO;
import kopo.userservice.service.IUserService;

@RestController
@RequestMapping("/user")
public class UserInfoController {
    private static final Logger log = LoggerFactory.getLogger(UserInfoController.class);

    private final IUserService userService;

    /**
     * 의존성 주입: 생성자 주입 방식 사용 (Clean Code 원칙)
     * @param userService 사용자 서비스 인터페이스
     */
    public UserInfoController(IUserService userService) {
        this.userService = userService;
    }

    /**
     * 현재 인증된 사용자의 정보를 조회합니다.
     * @param authentication Spring Security 인증 객체
     * @return userId, userName, userRole, managerId(관리자인 경우)를 포함하는 Map
     */
    @GetMapping("/me")
    public Map<String, Object> getMe(Authentication authentication) {
        log.info("[UserInfoController] GET /user/me called");

        Map<String, Object> responseMap = new HashMap<>(); // 가변 Map 사용

        if (authentication == null) {
            log.warn("인증(Authentication) 객체가 null입니다. 요청이 제대로 인증되지 않았습니다.");
            responseMap.put("userId", null);
            responseMap.put("userName", null);
            responseMap.put("userRole", null);
            responseMap.put("managerId", null); // managerId도 null로 설정
            return responseMap;
        }

        String userId = extractUserId(authentication);
        String userName = null;
        String userRole = null;
        String managerId = null; // managerId 변수 추가

        if (userId != null) {
            try {
                UserInfoDTO userInfo = userService.getUserInfo(userId);
                if (userInfo != null) {
                    userName = userInfo.getName();
                    userRole = convertUserTypeToRole(userInfo.getUserType());
                    // 💡 [수정] userType이 manager일 경우 managerId를 가져옴
                    if ("manager".equals(userInfo.getUserType())) {
                        managerId = userInfo.getManagerId();
                        log.debug("관리자 정보 확인됨: userId={}, managerId={}", userId, managerId);
                    }
                } else {
                    log.warn("DB에서 사용자 정보(userId: {})를 찾을 수 없습니다.", userId);
                }
            } catch (Exception e) {
                log.error("사용자 정보 조회 중 예외 발생: userId={}", userId, e);
                // 예외 발생 시 정보는 null로 유지
            }
        }

        // 🚨 [수정] userId, userName, userRole, managerId를 Map에 담아 반환
        responseMap.put("userId", userId);
        responseMap.put("userName", userName);
        responseMap.put("userRole", userRole);
        responseMap.put("managerId", managerId); // managerId 추가

        log.debug("Returning /user/me response: {}", responseMap); // 반환 값 로그 추가
        return responseMap;
    }

    /**
     * Principal 객체에서 userId를 추출합니다. (단일 책임 원칙 적용)
     * @param authentication 인증 객체
     * @return 추출된 userId (String)
     */
    private String extractUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        log.debug("Principal 객체 타입: {}", principal == null ? "null" : principal.getClass().getName());

        if (principal == null) {
            return null;
        }

        // Jwt 타입인지 먼저 확인 (가장 일반적인 경우)
        if (principal instanceof Jwt jwt) {
            // JWT 토큰에서 클레임 추출 (sub 클레임이 userId 역할)
            String userId = jwt.getSubject(); // 'sub' 클레임을 userId로 사용
            log.debug("JWT principal detected. Extracted userId (sub): {}", userId);
            return userId;
        } else if (principal instanceof String principalString) {
            // userId가 String으로 직접 넘어오는 경우 (테스트 또는 다른 인증 방식)
            log.debug("String principal detected: {}", principalString);
            return principalString;
        } else {
            // 기타 Principal 타입 처리 (toString 시도)
            try {
                String principalStr = principal.toString();
                log.debug("Other principal type detected. Using toString(): {}", principalStr);
                return principalStr;
            } catch (Exception e) {
                log.error("Principal.toString() 중 예외 발생: {}", principal, e);
                return null;
            }
        }
    }

    /**
     * 사용자 유형("patient", "manager")을 Spring Security 역할("ROLE_USER", "ROLE_USER_MANAGER")로 변환합니다.
     * @param userType 사용자 유형
     * @return 변환된 역할 문자열
     */
    private String convertUserTypeToRole(String userType) {
        if ("patient".equals(userType)) {
            return "ROLE_USER";
        } else if ("manager".equals(userType)) {
            return "ROLE_USER_MANAGER";
        }
        return null; // 매핑되지 않는 경우
    }

    /**
     * 사용자의 비밀번호를 확인합니다.
     * @param dto 사용자 ID와 비밀번호를 포함하는 DTO
     * @return 성공/실패 메시지를 담은 MsgDTO
     */
    @PostMapping("/verify-password")
    public MsgDTO verifyPassword(@RequestBody UserLoginRequestDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // [로그] 기존 System.out.println 대신 log.info 사용
        log.info("[UserInfoController] /user/verify-password called. 현재 인증된 사용자 권한: {}", auth.getAuthorities());

        boolean result = userService.verifyPassword(dto.getUserId(), dto.getPassword());

        if (result) {
            return MsgDTO.builder().result(1).msg("본인 확인 성공").build();
        } else {
            return MsgDTO.builder().result(0).msg("비밀번호가 올바르지 않습니다.").build();
        }
    }

    /**
     * 특정 사용자 ID의 상세 정보를 조회합니다.
     * @param userId 조회할 사용자 ID
     * @return 사용자 상세 정보 DTO
     */
    @GetMapping("/info")
    public UserInfoDTO getUserInfo(@RequestParam("userId") String userId) {
        log.info("[UserInfoController] GET /user/info called, userId={}", userId);
        return userService.getUserInfo(userId);
    }

    /**
     * 사용자의 이름을 변경합니다.
     * @param req userId와 newName을 포함하는 요청 맵
     * @return 성공/실패 메시지를 담은 MsgDTO
     */
    @PostMapping("/update-name")
    public MsgDTO updateName(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        String newName = req.get("newName");
        log.info("[UserInfoController] POST /user/update-name called, userId={}, newName={}", userId, newName);

        boolean result = userService.updateName(userId, newName);

        return MsgDTO.builder()
                .result(result ? 1 : 0)
                .msg(result ? "이름 변경 성공" : "이름 변경 실패")
                .accessToken(null)
                .build();
    }

    /**
     * 사용자의 이메일을 변경합니다.
     * @param req userId와 newEmail을 포함하는 요청 맵
     * @return 성공/실패 메시지를 담은 MsgDTO
     */
    @PostMapping("/update-email")
    public MsgDTO updateEmail(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        String newEmail = req.get("newEmail");
        log.info("[UserInfoController] POST /user/update-email called, userId={}, newEmail={}", userId, newEmail);

        boolean result = userService.updateEmail(userId, newEmail);

        return MsgDTO.builder()
                .result(result ? 1 : 0)
                .msg(result ? "이메일 변경 성공" : "이메일 변경 실패")
                .accessToken(null)
                .build();
    }

    /**
     * 사용자의 아이디를 변경합니다.
     * @param req userId와 newId를 포함하는 요청 맵
     * @return 성공/실패 메시지를 담은 MsgDTO
     */
    @PostMapping("/update-id")
    public MsgDTO updateId(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        String newId = req.get("newId");
        log.info("[UserInfoController] POST /user/update-id called, userId={}, newId={}", userId, newId);

        boolean result = userService.updateId(userId, newId);

        return MsgDTO.builder()
                .result(result ? 1 : 0)
                .msg(result ? "아이디 변경 성공" : "아이디 변경 실패")
                .accessToken(null)
                .build();
    }

    /**
     * 사용자의 비밀번호를 변경합니다.
     * @param req userId, currentPassword, newPassword를 포함하는 요청 맵
     * @return 성공/실패 메시지를 담은 MsgDTO
     */
    @PostMapping("/update-password")
    public MsgDTO updatePassword(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        String currentPassword = req.get("currentPassword");
        String newPassword = req.get("newPassword");
        log.info("[UserInfoController] POST /user/update-password called, userId={}", userId);

        boolean result = userService.updatePassword(userId, currentPassword, newPassword);

        return MsgDTO.builder()
                .result(result ? 1 : 0)
                .msg(result ? "비밀번호 변경 성공" : "비밀번호 변경 실패")
                .accessToken(null)
                .build();
    }

    /**
     * 사용자 회원 탈퇴를 처리합니다.
     * @param req userId를 포함하는 요청 맵
     * @return 성공/실패 메시지를 담은 MsgDTO
     */
    @PostMapping("/withdrawal")
    public MsgDTO withdrawal(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        log.info("[UserInfoController] POST /user/withdrawal called. 회원탈퇴 요청 userId={}", userId);

        // 서비스 계층에서 회원 탈퇴 로직 및 결과 MsgDTO 반환
        return userService.withdrawalUser(userId);
    }

    /**
     * 환자의 감지 범위 설정을 조회합니다.
     * @param authentication Spring Security 인증 객체
     * @return 감지 범위 설정을 포함하는 Map (hand, face, both)
     */
    @GetMapping("/detection-area")
    public Map<String, Boolean> getDetectionArea(Authentication authentication) {
        String userId = extractUserId(authentication);
        if (userId == null) {
            log.warn("[getDetectionArea] 인증된 사용자 ID를 찾을 수 없습니다.");
            return Collections.emptyMap();
        }
        log.info("[UserInfoController] GET /user/detection-area called for userId={}", userId);
        return userService.getDetectionArea(userId);
    }

    /**
     * 환자의 감지 범위 설정을 업데이트합니다.
     * @param authentication Spring Security 인증 객체
     * @param req detectionAreaType을 포함하는 요청 맵
     * @return 성공/실패 메시지를 담은 MsgDTO
     */
    @PostMapping("/update-detection-area")
    public MsgDTO updateDetectionArea(Authentication authentication, @RequestBody Map<String, String> req) {
        String userId = extractUserId(authentication);
        if (userId == null) {
            log.warn("[updateDetectionArea] 인증된 사용자 ID를 찾을 수 없습니다.");
            return MsgDTO.builder().result(0).msg("인증된 사용자 정보를 찾을 수 없습니다.").build();
        }
        String detectionAreaType = req.get("detectionAreaType");
        log.info("[UserInfoController] POST /user/update-detection-area called for userId={}, type={}", userId, detectionAreaType);

        boolean result = userService.updateDetectionArea(userId, detectionAreaType);

        return MsgDTO.builder()
                .result(result ? 1 : 0)
                .msg(result ? "감지 범위 변경 성공" : "감지 범위 변경 실패")
                .build();
    }
}
