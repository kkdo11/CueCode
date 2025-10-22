//package kopo.motionservice.config;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * 게이트웨이가 전달한 X-User-Id / X-Authorities 헤더를 이용해 인증을 복원하는 간단한 필터.
// * 주의: 내부 네트워크에서 게이트웨이를 신뢰할 수 있을 때만 사용하세요.
// */
//public class TrustedHeaderAuthenticationFilter extends OncePerRequestFilter {
//
//    private static final Logger log = LoggerFactory.getLogger(TrustedHeaderAuthenticationFilter.class);
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        String userId = request.getHeader("X-User-Id");
//        String authorities = request.getHeader("X-Authorities");
//
//        if (userId != null && !userId.isBlank()) {
//            List<SimpleGrantedAuthority> auths = List.of();
//            if (authorities != null && !authorities.isBlank()) {
//                auths = Arrays.stream(authorities.split(","))
//                        .map(String::trim)
//                        .filter(s -> !s.isEmpty())
//                        .map(SimpleGrantedAuthority::new)
//                        .collect(Collectors.toList());
//            }
//
//            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, auths);
//            SecurityContextHolder.getContext().setAuthentication(auth);
//            log.debug("Restored authentication from headers: user={} authorities={}", userId, authorities);
//        }
//
//        filterChain.doFilter(request, response);
//    }
//}
//
package kopo.motionservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 게이트웨이가 전달한 X-User-Id / X-Authorities 헤더를 이용해 인증을 복원하는 필터.
 * 게이트웨이 비밀키(X-Gateway-Secret)를 통해 요청의 신뢰성을 검증합니다.
 */
public class TrustedHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TrustedHeaderAuthenticationFilter.class);

    // Spring SecurityConfig에서 @Value("${gateway.trusted.secret}")로 주입받는 비밀키
    private final String gatewayTrustedSecret;
    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";

    /**
     * 필터 생성자: SecurityConfig에서 @Value로 설정된 비밀키를 주입받습니다.
     * @param gatewayTrustedSecret 애플리케이션 설정 파일에 정의된 공유 비밀키
     */
    public TrustedHeaderAuthenticationFilter(String gatewayTrustedSecret) {
        // null 또는 빈 문자열을 방지하고 항상 비교 가능하도록 초기화
        this.gatewayTrustedSecret = (gatewayTrustedSecret == null) ? "" : gatewayTrustedSecret.trim();
        log.info("TrustedHeaderAuthenticationFilter initialized. Secret status: {}",
                this.gatewayTrustedSecret.isBlank() ? "BLANK (DANGER!)" : "SET");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. 게이트웨이 신뢰성 검증: X-Gateway-Secret 헤더 확인
        String requestSecret = request.getHeader(GATEWAY_SECRET_HEADER);

        // 환경 변수로 비밀키가 설정되어 있고 (실제 운영 환경), 요청 헤더의 비밀키와 일치하지 않으면 401 오류 반환
        if (!this.gatewayTrustedSecret.isBlank() && !this.gatewayTrustedSecret.equals(requestSecret)) {
            log.warn("Unauthorized access: Mismatched or missing trusted secret header. Request secret: {}", requestSecret);
            // 401 UNAUTHORIZED 반환
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid Gateway Secret");
            return;
        }

        // 2. 인증 정보 추출 및 등록
        String userId = request.getHeader("X-User-Id");
        String authorities = request.getHeader("X-Authorities");

        // 신뢰성 검증을 통과하고, 사용자 ID가 있을 경우에만 인증 처리
        if (userId != null && !userId.isBlank()) {
            List<SimpleGrantedAuthority> auths = List.of();

            // 권한(Authorities) 정보 파싱 (콤마로 구분된 문자열을 List<SimpleGrantedAuthority>로 변환)
            if (authorities != null && !authorities.isBlank()) {
                auths = Arrays.stream(authorities.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            // Spring Security Context에 인증 정보 등록
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, auths);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Restored authentication from headers: user={} authorities={}", userId, authorities);
        } else if (!this.gatewayTrustedSecret.isBlank()) {
            // 신뢰성 검증은 통과했지만, 사용자 ID 헤더가 없는 경우 (경고성 로그)
            log.debug("Trusted request did not contain X-User-Id. Proceeding without authentication.");
        }

        // 다음 필터 체인으로 요청 전달
        filterChain.doFilter(request, response);
    }
}
