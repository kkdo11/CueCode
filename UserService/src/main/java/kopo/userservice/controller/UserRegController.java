package kopo.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import kopo.userservice.dto.MsgDTO;
import kopo.userservice.dto.PatientDTO;
import kopo.userservice.service.IUserService;
import kopo.userservice.util.CmmUtil;
import kopo.userservice.util.EncryptUtil;
import kopo.userservice.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "회원가입을 위한 API", description = "회원가입을 위한 API 설명입니다.")
@Slf4j
@RequestMapping(value = "/reg")
@RequiredArgsConstructor
@RestController
public class UserRegController {
    private final IUserService userService;
    private final PasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private kopo.userservice.repository.PatientRepository patientRepository;
    @Autowired
    private kopo.userservice.repository.ManagerRepository managerRepository;

    @Operation(summary = "환자 회원가입 API", description = "환자 회원가입 API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Page Not Found!"),
            }
    )
    @PostMapping(value = "insertPatient")
    public MsgDTO insertPatient(HttpServletRequest request) {
        log.info(this.getClass().getName() + ".insertPatient start!");
        int res = 0;
        String msg = "";
        MsgDTO dto;
        PatientDTO pDTO;
        try {
            String userId = CmmUtil.nvl(request.getParameter("user_id"));
            String userName = CmmUtil.nvl(request.getParameter("user_name"));
            String password = CmmUtil.nvl(request.getParameter("password"));
            String email = CmmUtil.nvl(request.getParameter("email"));
            String detectionAreaType = CmmUtil.nvl(request.getParameter("detectionAreaType")); // hand/face/both
            log.info("userId : " + userId);
            log.info("userName : " + userName);
            log.info("password : " + password);
            log.info("email : " + email);
            log.info("detectionAreaType : " + detectionAreaType);
            pDTO = PatientDTO.builder()
                    .id(userId)
                    .pw(bCryptPasswordEncoder.encode(password))
                    .email(EncryptUtil.encAES128CBC(email))
                    .name(userName)
                    .managerIds(java.util.Collections.emptyList())
                    .detectionAreaType(detectionAreaType)
                    .build();
            res = userService.insertPatient(pDTO);
            log.info("회원가입 결과(res) : " + res);
            if (res == 1) {
                msg = "회원가입되었습니다.";
            } else if (res == 2) {
                msg = "이미 가입된 아이디입니다.";
            } else {
                msg = "오류로 인해 회원가입이 실패하였습니다.";
            }
        } catch (Exception e) {
            msg = "실패하였습니다. : " + e;
            res = 2;
            log.info(e.toString());
        } finally {
            dto = MsgDTO.builder().result(res).msg(msg).build();
            log.info(this.getClass().getName() + ".insertPatient End!");
        }
        return dto;
    }

    @Operation(summary = "관리자 회원가입 API", description = "관리자 회원가입 API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Page Not Found!"),
            }
    )
    @PostMapping(value = "insertManager")
    public MsgDTO insertManager(HttpServletRequest request) {
        log.info(this.getClass().getName() + ".insertManager start!");
        int res = 0;
        String msg = "";
        MsgDTO dto;
        kopo.userservice.dto.ManagerDTO mDTO;
        try {
            String userId = CmmUtil.nvl(request.getParameter("user_id"));
            String userName = CmmUtil.nvl(request.getParameter("user_name"));
            String password = CmmUtil.nvl(request.getParameter("password"));
            String email = CmmUtil.nvl(request.getParameter("email"));
            log.info("userId : " + userId);
            log.info("userName : " + userName);
            log.info("password : " + password);
            log.info("email : " + email);
            mDTO = kopo.userservice.dto.ManagerDTO.builder()
                    .id(userId)
                    .pw(bCryptPasswordEncoder.encode(password))
                    .email(EncryptUtil.encAES128CBC(email))
                    .name(userName)
                    .patientIds(java.util.Collections.emptyList())
                    .build();
            res = userService.insertManager(mDTO);
            log.info("관리자 회원가입 결과(res) : " + res);
            if (res == 1) {
                msg = "관리자 회원가입되었습니다.";
            } else if (res == 2) {
                msg = "이미 가입된 아이디입니다.";
            } else {
                msg = "오류로 인해 관리자 회원가입이 실패하였습니다.";
            }
        } catch (Exception e) {
            msg = "실패하였습니다. : " + e;
            res = 2;
            log.info(e.toString());
        } finally {
            dto = MsgDTO.builder().result(res).msg(msg).build();
            log.info(this.getClass().getName() + ".insertManager End!");
        }
        return dto;
    }

    @Operation(summary = "환자 정보 조회 API", description = "환자 정보 조회 API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Page Not Found!"),
            }
    )
    @PostMapping("patientInfo")
    public MsgDTO patientInfo(HttpServletRequest request) {
        log.info(this.getClass().getName() + ".patientInfo start!");
        int res = 0;
        String msg = "";
        MsgDTO dto;
        try {
            String userId = CmmUtil.nvl(request.getParameter("user_id"));
            // 실제 환자 정보 조회 로직 구현 필요
            msg = "환자 정보 조회 성공: " + userId;
            res = 1;
        } catch (Exception e) {
            msg = "실패하였습니다. : " + e;
            res = 2;
            log.info(e.toString());
        } finally {
            dto = MsgDTO.builder().result(res).msg(msg).build();
            log.info(this.getClass().getName() + ".patientInfo End!");
        }
        return dto;
    }

    @Operation(summary = "관리자 정보 조회 API", description = "관리자 정보 조회 API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Page Not Found!"),
            }
    )
    @PostMapping("managerInfo")
    public MsgDTO managerInfo(HttpServletRequest request) {
        log.info(this.getClass().getName() + ".managerInfo start!");
        int res = 0;
        String msg = "";
        MsgDTO dto;
        try {
            String userId = CmmUtil.nvl(request.getParameter("user_id"));
            // 실제 관리자 정보 조회 로직 구현 필요
            msg = "관리자 정보 조회 성공: " + userId;
            res = 1;
        } catch (Exception e) {
            msg = "실패하였습니다. : " + e;
            res = 2;
            log.info(e.toString());
        } finally {
            dto = MsgDTO.builder().result(res).msg(msg).build();
            log.info(this.getClass().getName() + ".managerInfo End!");
        }
        return dto;
    }

    @Operation(summary = "환자 정보 조회 API (/user/patientInfo)", description = "환자 정보 조회 API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Page Not Found!"),
            }
    )
    @PostMapping("/user/patientInfo")
    public MsgDTO userPatientInfo(HttpServletRequest request) {
        log.info(this.getClass().getName() + ".userPatientInfo start!");
        int res = 0;
        String msg = "";
        MsgDTO dto;
        try {
            String userId = CmmUtil.nvl(request.getParameter("user_id"));
            msg = "(user) 환자 정보 조회 성공: " + userId;
            res = 1;
        } catch (Exception e) {
            msg = "실패하였습니다. : " + e;
            res = 2;
            log.info(e.toString());
        } finally {
            dto = MsgDTO.builder().result(res).msg(msg).build();
            log.info(this.getClass().getName() + ".userPatientInfo End!");
        }
        return dto;
    }

    @Operation(summary = "관리자 정보 조회 API (/user/managerInfo)", description = "관리자 정보 조회 API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Page Not Found!"),
            }
    )
    @PostMapping("/user/managerInfo")
    public MsgDTO userManagerInfo(HttpServletRequest request) {
        log.info(this.getClass().getName() + ".userManagerInfo start!");
        int res = 0;
        String msg = "";
        MsgDTO dto;
        try {
            String userId = CmmUtil.nvl(request.getParameter("user_id"));
            msg = "(user) 관리자 정보 조회 성공: " + userId;
            res = 1;
        } catch (Exception e) {
            msg = "실패하였습니다. : " + e;
            res = 2;
            log.info(e.toString());
        } finally {
            dto = MsgDTO.builder().result(res).msg(msg).build();
            log.info(this.getClass().getName() + ".userManagerInfo End!");
        }
        return dto;
    }

    @Operation(summary = "아이디 중복확인 API", description = "아이디 중복확인 API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Page Not Found!"),
            }
    )
    @RequestMapping(value = "checkUserId", method = org.springframework.web.bind.annotation.RequestMethod.GET)
    public java.util.Map<String, Object> checkUserId(HttpServletRequest request) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            String userId = kopo.userservice.util.CmmUtil.nvl(request.getParameter("user_id"));
            log.info("[checkUserId] user_id 파라미터: {}", userId);
            if (userId == null || userId.isBlank()) {
                result.put("available", false);
                result.put("error", "user_id 파라미터가 비어있음");
                return result;
            }
            boolean available = !userService.existsUserId(userId);
            result.put("available", available);
        } catch (Exception e) {
            log.error("[checkUserId] 예외 발생", e);
            result.put("available", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "인증메일 발송 API", description = "이메일 인증을 위한 메일을 발송합니다.")
    @PostMapping(value = "sendMail")
    public MsgDTO sendMail(@RequestBody Map<String, String> req) {
        log.info(this.getClass().getName() + ".sendMail start!");
        String email = req.getOrDefault("email", "").trim();
        if (email.isEmpty()) {
            log.warn("[sendMail] 이메일 파라미터가 비어 있습니다.");
            return MsgDTO.builder().result(0).msg("이메일 주소가 입력되지 않았습니다.").build();
        }
        int res;
        String msg;
        try {
            int authCode = userService.sendEmailAuthCode(email);
            res = 1;
            msg = "메일 발송 성공, 인증번호: " + authCode;
        } catch (Exception e) {
            res = 0;
            msg = "메일 발송 실패: " + e.getMessage();
        }
        return MsgDTO.builder().result(res).msg(msg).build();
    }

    @PostMapping(value = "sendEmailAuth")
    public MsgDTO sendEmailAuth(HttpServletRequest request) {
        log.info(this.getClass().getName() + ".sendEmailAuth start!");

        // 원본 이메일 로깅
        String originalEmail = CmmUtil.nvl(request.getParameter("email"));
        log.info("[sendEmailAuth] 원본 이메일: {}", originalEmail);

        // 이메일 정규화 및 로깅
        String normalizedEmail = originalEmail.trim().toLowerCase();
        log.info("[sendEmailAuth] 정규화된 이메일: {}", normalizedEmail);

        String key = "emailAuth:" + normalizedEmail;
        int authCode;
        String msg;
        try {
            authCode = userService.sendEmailAuthCode(normalizedEmail);
            log.info("[sendEmailAuth] Redis 저장 key: {} / value: {}", key, authCode);

            // 저장 확인
            String savedValue = redisUtil.get(key);
            log.info("[sendEmailAuth] Redis 저장 확인: {} = {}", key, savedValue);

            msg = "메일 발송 성공";
        } catch (Exception e) {
            log.error("[sendEmailAuth] 인증번호 발송 실패", e);
            msg = "메일 발송 실패: " + e.getMessage();
            return MsgDTO.builder().result(0).msg(msg).build();
        }
        return MsgDTO.builder().result(1).msg(msg).build();
    }

    @PostMapping(value = "verifyEmailAuth")
    public MsgDTO verifyEmailAuth(HttpServletRequest request, @RequestBody(required = false) Map<String, String> body) {
        log.info(this.getClass().getName() + ".verifyEmailAuth start!");

        // 이메일 정보 처리 - form 파라미터 또는 JSON 바디에서 가져옴
        String originalEmail;
        String inputCode;

        // RequestBody로 들어온 경우
        if (body != null && !body.isEmpty()) {
            originalEmail = CmmUtil.nvl(body.get("email"));
            inputCode = CmmUtil.nvl(body.get("authCode"));
            log.info("[verifyEmailAuth] RequestBody로 받은 파라미터: email={}, authCode={}", originalEmail, inputCode);
        } else {
            // form 파라미터로 들어온 경우
            originalEmail = CmmUtil.nvl(request.getParameter("email"));
            inputCode = CmmUtil.nvl(request.getParameter("authCode"));
            log.info("[verifyEmailAuth] form 파라미터로 받은 파라미터: email={}, authCode={}", originalEmail, inputCode);
        }

        log.info("[verifyEmailAuth] 원본 이메일: {}", originalEmail);

        // 이메일 정규화 및 로깅
        String normalizedEmail = originalEmail.trim().toLowerCase();
        log.info("[verifyEmailAuth] 정규화된 이메일: {}", normalizedEmail);
        log.info("[verifyEmailAuth] 입력된 인증번호: {}", inputCode);

        String key = "emailAuth:" + normalizedEmail;
        log.info("[verifyEmailAuth] 조회할 Redis 키: {}", key);

        String redisCode = redisUtil.get(key);
        log.info("[verifyEmailAuth] Redis 조회 결과: {} = {}", key, redisCode);

        String msg;
        int result = 0;
        if (redisCode == null) {
            log.warn("[verifyEmailAuth] 인증번호가 Redis에 없음 (만료 또는 저장 실패)");
            msg = "인증번호가 만료되었거나 존재하지 않습니다. 인증번호를 다시 요청해주세요.";
        } else {
            boolean isNumeric = inputCode != null && !inputCode.isBlank() && inputCode.matches("\\d+");
            if (isNumeric && (inputCode.equals(redisCode) || String.valueOf(Integer.parseInt(inputCode)).equals(redisCode))) {
                // 문자열 비교와 숫자 변환 후 비교 모두 시도
                log.info("[verifyEmailAuth] 인증번호 일치 확인: {} == {}", inputCode, redisCode);
                msg = "인증번호가 일치합니다.";
                result = 1;
                redisUtil.delete(key); // 인증 성공 시 인증번호 삭제
                log.info("[verifyEmailAuth] 인증 성공으로 Redis 키 삭제: {}", key);
            } else {
                log.warn("[verifyEmailAuth] 인증번호 불일치: 입력값={}, 저장값={}", inputCode, redisCode);
                msg = "인증번호가 일치하지 않습니다.";
            }
        }

        return MsgDTO.builder().result(result).msg(msg).build();
    }

    @PostMapping("/find/resetPassword")
    public Map<String, Object> resetPassword(@RequestBody Map<String, String> req) {
        String userId = req.get("user_id");
        String email = req.get("email");
        String newPassword = req.get("new_password");
        String role = req.get("role"); // "patient" 또는 "manager"
        Map<String, Object> result = new HashMap<>();

        String encryptedEmail = "";
        try {
            encryptedEmail = EncryptUtil.encAES128CBC(email);
        } catch (Exception e) {
            result.put("result", 0);
            result.put("msg", "이메일 암호화 오류: " + e.getMessage());
            return result;
        }
        boolean success = false;
        if ("manager".equalsIgnoreCase(role)) {
            kopo.userservice.model.ManagerDocument manager = managerRepository.findByIdAndEmail(userId, encryptedEmail);
            if (manager != null) {
                manager.setPw(bCryptPasswordEncoder.encode(newPassword));
                managerRepository.save(manager);
                success = true;
            }
        } else {
            kopo.userservice.model.PatientDocument patient = patientRepository.findByIdAndEmail(userId, encryptedEmail);
            if (patient != null) {
                patient.setPw(bCryptPasswordEncoder.encode(newPassword));
                patientRepository.save(patient);
                success = true;
            }
        }
        if (success) {
            result.put("result", 1);
        } else {
            result.put("result", 0);
            result.put("msg", "일치하는 회원 정보가 없습니다.");
        }
        return result;
    }

    @Operation(summary = "아이디 찾기 API", description = "이메일로 사용자 아이디를 찾습니다.")
    @PostMapping("/findIdByEmail")
    public Map<String, Object> findIdByEmail(@RequestBody Map<String, String> req) {
        log.info(this.getClass().getName() + ".findIdByEmail start!");
        Map<String, Object> result = new HashMap<>();
        String email = req.getOrDefault("email", "").trim();

        if (email.isEmpty()) {
            log.warn("[findIdByEmail] 이메일 파라미터가 비어 있습니다.");
            result.put("result", 0);
            result.put("msg", "이메일 주소가 입력되지 않았습니다.");
            return result;
        }

        try {
            String userId = userService.findUserIdByEmail(email);
            if (userId != null) {
                result.put("result", 1);
                result.put("msg", "아이디를 찾았습니다.");
                result.put("userId", userId);
                log.info("[findIdByEmail] 아이디 찾기 성공: email={}, userId={}", email, userId);
            }
            else {
                result.put("result", 0);
                result.put("msg", "일치하는 아이디를 찾을 수 없습니다.");
                log.info("[findIdByEmail] 아이디 찾기 실패: email={}", email);
            }
        } catch (Exception e) {
            log.error("[findIdByEmail] 아이디 찾기 중 예외 발생", e);
            result.put("result", 0);
            result.put("msg", "아이디 찾기 중 오류 발생: " + e.getMessage());
        } finally {
            log.info(this.getClass().getName() + ".findIdByEmail End!");
        }
        return result;
    }
}
