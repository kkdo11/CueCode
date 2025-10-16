package kopo.userservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import kopo.userservice.dto.UserLoginRequestDTO;
import kopo.userservice.dto.MsgDTO;
import kopo.userservice.service.IUserService;
import org.springframework.web.bind.annotation.RequestParam;
import kopo.userservice.dto.UserInfoDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/user")
public class UserInfoController {
    private static final Logger log = LoggerFactory.getLogger(UserInfoController.class);
    @Autowired
    private IUserService userService;
    @GetMapping("/me")
    public Map<String, Object> getMe(org.springframework.security.core.Authentication authentication) {
        log.info("[UserInfoController] /user/me called");
        String managerId = null;
        try {
            if (authentication == null) {
                log.warn("Authentication is null!");
                return Map.of("managerId", null);
            }
            Object principal = authentication.getPrincipal();
            log.info("Authentication: {}", authentication);
            log.info("Principal: {}", principal);
            log.info("Principal class: {}", principal == null ? "null" : principal.getClass().getName());
            if (principal instanceof String) {
                managerId = (String) principal;
                log.info("Principal is String, managerId: {}", managerId);
            } else if (principal instanceof Jwt jwt) {
                managerId = jwt.getClaimAsString("managerId");
                log.info("Principal is Jwt, managerId: {}", managerId);
            } else {
                try {
                    managerId = principal != null ? principal.toString() : null;
                    log.info("Principal is Other, managerId: {}", managerId);
                } catch (Exception e) {
                    log.error("Principal toString error", e);
                }
            }
        } catch (Exception e) {
            log.error("/user/me 예외 발생", e);
            managerId = null;
        }
        return Map.of("managerId", managerId);
    }
    @PostMapping("/verify-password")
    public MsgDTO verifyPassword(@RequestBody UserLoginRequestDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("[로그] 현재 인증된 사용자 권한: " + auth.getAuthorities());
        boolean result = userService.verifyPassword(dto.getUserId(), dto.getPassword());
        if (result) {
            return MsgDTO.builder().result(1).msg("본인 확인 성공").build();
        } else {
            return MsgDTO.builder().result(0).msg("비밀번호가 올바르지 않습니다.").build();
        }
    }
    @GetMapping("/info")
    public UserInfoDTO getUserInfo(@RequestParam("userId") String userId) {
        log.info("[UserInfoController] /user/info called, userId={}", userId);
        return userService.getUserInfo(userId);
    }

    @PostMapping("/update-name")
    public MsgDTO updateName(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        String newName = req.get("newName");
        boolean result = userService.updateName(userId, newName);
        return MsgDTO.builder()
            .result(result ? 1 : 0)
            .msg(result ? "이름 변경 성공" : "이름 변경 실패")
            .accessToken(null)
            .build();
    }

    @PostMapping("/update-email")
    public MsgDTO updateEmail(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        String newEmail = req.get("newEmail");
        boolean result = userService.updateEmail(userId, newEmail);
        return MsgDTO.builder()
            .result(result ? 1 : 0)
            .msg(result ? "이메일 변경 성공" : "이메일 변경 실패")
            .accessToken(null)
            .build();
    }

    @PostMapping("/update-id")
    public MsgDTO updateId(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        String newId = req.get("newId");
        boolean result = userService.updateId(userId, newId);
        return MsgDTO.builder()
            .result(result ? 1 : 0)
            .msg(result ? "아이디 변경 성공" : "아이디 변경 실패")
            .accessToken(null)
            .build();
    }

    @PostMapping("/update-password")
    public MsgDTO updatePassword(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        String currentPassword = req.get("currentPassword");
        String newPassword = req.get("newPassword");
        boolean result = userService.updatePassword(userId, currentPassword, newPassword);
        return MsgDTO.builder()
            .result(result ? 1 : 0)
            .msg(result ? "비밀번호 변경 성공" : "비밀번호 변경 실패")
            .accessToken(null)
            .build();
    }

    @PostMapping("/withdrawal")
    public MsgDTO withdrawal(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        log.info("[UserInfoController] 회원탈퇴 요청 userId={}", userId);
        return userService.withdrawalUser(userId);
    }
}
