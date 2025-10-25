package kopo.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import kopo.userservice.auth.AuthInfo;
import kopo.userservice.auth.JwtTokenProvider;
import kopo.userservice.auth.JwtTokenType;
import kopo.userservice.dto.MsgDTO;
import kopo.userservice.dto.PatientDTO;
import kopo.userservice.dto.ManagerDTO;
import kopo.userservice.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.Optional;

@Tag(name = "로그인 관련 API", description = "로그인 관련 API 설명입니다.")
@Slf4j
@RequestMapping(value = "/login")
@RequiredArgsConstructor
@RestController
public class LoginController {

    @Value("${jwt.token.access.valid.time}")
    private long accessTokenValidTime;

    @Value("${jwt.token.access.name}")
    private String accessTokenName;

    @Value("${jwt.token.refresh.valid.time}")
    private long refreshTokenValidTime;

    @Value("${jwt.token.refresh.name}")
    private String refreshTokenName;

    @Value("${server.ssl.enabled:false}")
    private boolean isSslEnabled;

    private final JwtTokenProvider jwtTokenProvider;
    private final kopo.userservice.service.IUserService userService;
    private final kopo.userservice.util.RedisUtil redisUtil;

    @Operation(summary = "로그인 성공처리  API", description = "로그인 성공처리 API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Page Not Found!"),
            }
    )
    @PostMapping(value = "loginSuccess")
    public MsgDTO loginSuccess(@AuthenticationPrincipal AuthInfo authInfo,
                               HttpServletResponse response) {
        log.info(this.getClass().getName() + ".loginSuccess Start!");

        // 인증 객체에서 환자/관리자 DTO 추출
        Object userDto = authInfo != null ? authInfo.user() : null;
        String userId = "";
        String userName = "";
        String userRoles = "";
        String managerId = "";
        if (userDto instanceof PatientDTO dto) {
            userId = CmmUtil.nvl(dto.id());
            userName = CmmUtil.nvl(dto.name());
            userRoles = "ROLE_USER"; // 환자 권한
        } else if (userDto instanceof ManagerDTO dto) {
            userId = CmmUtil.nvl(dto.id());
            userName = CmmUtil.nvl(dto.name());
            userRoles = "ROLE_USER_MANAGER"; // 관리자 권한
            managerId = CmmUtil.nvl(dto.id()); // managerId 값 추출
        }
        log.info("userId : {}", userId);
        log.info("userName : {}", userName);
        log.info("userRoles : {}", userRoles);
        log.info("managerId : {}", managerId); // managerId 값 로그 추가

        // Access Token 생성
        String accessToken = jwtTokenProvider.createToken(userId, userName, userRoles, managerId, JwtTokenType.ACCESS_TOKEN);
        log.info("accessToken : {}", accessToken);
        log.info("[Redis] access 저장 시도: key={}, value={}, ttl={}", "access:" + userId, accessToken, accessTokenValidTime);
        redisUtil.set("access:" + userId, accessToken, accessTokenValidTime);

        String sameSite = isSslEnabled ? "None" : "Lax";

        ResponseCookie accessTokenCookie = ResponseCookie.from(accessTokenName, accessToken)
                .path("/")
                .maxAge(accessTokenValidTime)
                .httpOnly(true)
                .secure(isSslEnabled)
                .sameSite(sameSite)
                .build();
        response.setHeader("Set-Cookie", accessTokenCookie.toString());

        // Refresh Token 생성
        String refreshToken = jwtTokenProvider.createToken(userId, userName, userRoles, managerId, JwtTokenType.REFRESH_TOKEN);
        log.info("refreshToken : {}", refreshToken);
        log.info("[Redis] refresh 저장 시도: key={}, value={}, ttl={}", "refresh:" + userId, refreshToken, refreshTokenValidTime);
        redisUtil.set("refresh:" + userId, refreshToken, refreshTokenValidTime);

        ResponseCookie refreshTokenCookie = ResponseCookie.from(refreshTokenName, refreshToken)
                .path("/")
                .maxAge(refreshTokenValidTime)
                .httpOnly(true)
                .secure(isSslEnabled)
                .sameSite(sameSite)
                .build();
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());

        // 결과 메시지 전달하기
        MsgDTO dto = MsgDTO.builder().result(1).msg(userName + "님 로그인이 성공하였습니다.").build();
        log.info(this.getClass().getName() + ".loginSuccess End!");
        return dto;
    }

    @Operation(summary = "로그인 실패처리  API", description = "로그인 실패처리 API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Page Not Found!"),
            }
    )
    @PostMapping(value = "loginFail")
    public MsgDTO loginFail() {
        log.info(this.getClass().getName() + ".loginFail Start!");
        MsgDTO dto = MsgDTO.builder().result(1).msg("로그인이 실패하였습니다.").build();
        log.info(this.getClass().getName() + ".loginFail End!");
        return dto;
    }





    @Operation(summary = "로그아웃 처리 API", description = "JWT 토큰 쿠키 삭제를 통한 로그아웃 처리 API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Page Not Found!"),
            }
    )
    @PostMapping(value = "/logout")
    public MsgDTO logout(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
        log.info(this.getClass().getName() + ".logout Start!");

        // accessToken 쿠키에서 추출
        String accessToken = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(accessTokenName)) {
                    accessToken = cookie.getValue();
                    break;
                }
            }
        }
        log.info("[Logout] accessToken 추출 결과: {}", accessToken);
        String userId = null;
        if (accessToken != null && !accessToken.isEmpty()) {
            userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        }
        log.info("[Logout] userId 추출 결과: {}", userId);
        if (userId != null) {
            log.info("[Logout] Redis 삭제 시도: access:{}, refresh:{}", "access:" + userId, "refresh:" + userId);
            redisUtil.delete("access:" + userId);
            redisUtil.delete("refresh:" + userId);
            log.info("[Redis] 로그아웃: access:{} refresh:{} 삭제 시도 완료", userId, userId);
        } else {
            log.warn("[Logout] userId가 null이므로 Redis 삭제를 수행하지 않음");
        }

        userService.invalidateRefreshToken(request);

        String sameSite = isSslEnabled ? "Strict" : "Lax";

        // Access Token 쿠키 삭제
        ResponseCookie accessCookie = ResponseCookie.from(accessTokenName, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(isSslEnabled)
                .sameSite(sameSite)
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());

        // Refresh Token 쿠키 삭제
        ResponseCookie refreshCookie = ResponseCookie.from(refreshTokenName, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(isSslEnabled)
                .sameSite(sameSite)
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        MsgDTO dto = MsgDTO.builder().result(1).msg("로그아웃이 정상적으로 처리되었습니다.").build();
        log.info(this.getClass().getName() + ".logout End!");
        return dto;
    }

    @Operation(summary = "로그인 API", description = "user_id와 password로 로그인합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
            }
    )
    @PostMapping("")
    public MsgDTO login(@RequestBody kopo.userservice.dto.UserLoginRequestDTO loginRequest, HttpServletResponse response) {
        log.info("[POST] /login - user_id: {}", loginRequest.getUserId());
        Object userDto = userService.login(loginRequest.getUserId(), loginRequest.getPassword());
        if (userDto == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return MsgDTO.builder().result(0).msg("로그인 실패: 아이디 또는 비밀번호가 올바르지 않습니다.").build();
        }
        String userId = "";
        String userName = "";
        String userRoles = "";
        String managerId = "";
        if (userDto instanceof PatientDTO dto) {
            userId = CmmUtil.nvl(dto.id());
            userName = CmmUtil.nvl(dto.name());
            userRoles = "ROLE_USER"; // 환자 권한
        } else if (userDto instanceof ManagerDTO dto) {
            userId = CmmUtil.nvl(dto.id());
            userName = CmmUtil.nvl(dto.name());
            userRoles = "ROLE_USER_MANAGER"; // 관리자 권한
            managerId = CmmUtil.nvl(dto.id()); // managerId 값 추출
        }
        // JWT 토큰 생성 및 쿠키 설정
        String accessToken = jwtTokenProvider.createToken(userId, userName, userRoles, managerId, JwtTokenType.ACCESS_TOKEN);
        log.info("JWT 생성: userId={}, roles={}", userId, userRoles);
        String sameSite = isSslEnabled ? "None" : "Lax";

        ResponseCookie accessTokenCookie = ResponseCookie.from(accessTokenName, accessToken)
                .path("/")
                .maxAge(accessTokenValidTime)
                .httpOnly(true)
                .secure(isSslEnabled)
                .sameSite(sameSite)
                .build();
        response.setHeader("Set-Cookie", accessTokenCookie.toString());

        String refreshToken = jwtTokenProvider.createToken(userId, userName, userRoles, managerId, JwtTokenType.REFRESH_TOKEN);

        ResponseCookie refreshTokenCookie = ResponseCookie.from(refreshTokenName, refreshToken)
                .path("/")
                .maxAge(refreshTokenValidTime)
                .httpOnly(true)
                .secure(isSslEnabled)
                .sameSite(sameSite)
                .build();
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());
        log.info("[Redis] access 저장 시도: key={}, value={}, ttl={}", "access:" + userId, accessToken, accessTokenValidTime);
        redisUtil.set("access:" + userId, accessToken, accessTokenValidTime);
        log.info("[Redis] refresh 저장 시도: key={}, value={}, ttl={}", "refresh:" + userId, refreshToken, refreshTokenValidTime);
        redisUtil.set("refresh:" + userId, refreshToken, refreshTokenValidTime);
        // accessToken을 응답 JSON에 포함
        return MsgDTO.builder().result(1).msg(userName + "님 로그인이 성공하였습니다.").userName(userName).build();
    }
}
