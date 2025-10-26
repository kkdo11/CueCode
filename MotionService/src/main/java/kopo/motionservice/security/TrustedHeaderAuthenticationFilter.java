package kopo.motionservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 게이트웨이가 주입한 신뢰 헤더(X-User-Id, X-Authorities)를 읽어
 * SecurityContext에 Authentication을 복원합니다.
 *
 * 선택적으로 X-Gateway-Secret 값과 비교하여 게이트웨이인지 확인할 수 있습니다.
 */
public class TrustedHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TrustedHeaderAuthenticationFilter.class);

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String AUTHORITIES_HEADER = "X-Authorities";
    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";

    private final String gatewayTrustedSecret;

    public TrustedHeaderAuthenticationFilter(String gatewayTrustedSecret) {
        this.gatewayTrustedSecret = gatewayTrustedSecret == null ? "" : gatewayTrustedSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 상세 디버깅: 모든 헤더 값 출력
            java.util.Collections.list(request.getHeaderNames()).forEach(headerName -> {
                logger.debug("Header: {} = {}", headerName, request.getHeader(headerName));
            });

            // 디버깅: 주요 헤더 값 출력
            logger.debug("X-User-Id: {}, X-Authorities: {}, X-Gateway-Secret: {}",
                    request.getHeader(USER_ID_HEADER),
                    request.getHeader(AUTHORITIES_HEADER),
                    request.getHeader(GATEWAY_SECRET_HEADER));

            // 이미 인증 정보가 있으면 아무것도 안 함
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String userId = request.getHeader(USER_ID_HEADER);
                String authorities = request.getHeader(AUTHORITIES_HEADER);
                String gwSecret = request.getHeader(GATEWAY_SECRET_HEADER);

                if (userId != null) {
                    // 게이트웨이 시크릿이 설정되어 있으면 검증
                    if (!gatewayTrustedSecret.isBlank()) {
                        if (gwSecret == null || !gatewayTrustedSecret.equals(gwSecret)) {
                            logger.debug("Gateway secret mismatch or missing - rejecting header-based auth");
                        } else {
                            setAuthenticationFromHeaders(userId, authorities);
                        }
                    } else {
                        setAuthenticationFromHeaders(userId, authorities);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("TrustedHeaderAuthenticationFilter error: {}", e.getMessage());
            // 인증 복원 실패 시에도 요청 처리는 계속하게 하여 최종적으로 Spring Security가 접근을 차단하게 함
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthenticationFromHeaders(String userId, String authoritiesHeader) {
        List<SimpleGrantedAuthority> auths = List.of();
        if (authoritiesHeader != null && !authoritiesHeader.isBlank()) {
            auths = Arrays.stream(authoritiesHeader.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.startsWith("ROLE_") ? s : "ROLE_" + s) // ROLE_ 접두사 보장
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, auths);
        SecurityContextHolder.getContext().setAuthentication(auth);
        logger.debug("Restored authentication from headers; userId={}, authorities={}", userId, auths);
    }
}
