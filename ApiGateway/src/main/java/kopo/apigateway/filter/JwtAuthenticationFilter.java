package kopo.apigateway.filter;

import kopo.apigateway.jwt.JwtStatus;
import kopo.apigateway.jwt.JwtTokenProvider;
import kopo.apigateway.jwt.JwtTokenType;
import kopo.apigateway.dto.TokenDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${gateway.trusted.secret:}")
    private String gatewayTrustedSecret;

    // JWT 인증 필터를 건너뛸 경로 목록
    private final List<String> skipPaths = List.of(
            "/login",
            "/reg",
            "/actuator",
            "/api/login",
            "/api/reg"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        log.info("[JwtFilter] --------------------------------------------------");
        log.info("[JwtFilter] Request Path: {}", path);

        // skipPaths에 해당하는 경로는 JWT 인증 필터를 건너뜀
        if (skipPaths.stream().anyMatch(path::startsWith)) {
            log.info("[JwtFilter] Path is in skipPaths. Skipping filter.");
            return chain.filter(exchange);
        }

        String token = null;
        try {
            token = jwtTokenProvider.resolveToken(request, JwtTokenType.ACCESS_TOKEN);
            log.info("[JwtFilter] Resolved Token: {}", (token != null && !token.isEmpty()) ? "Present" : "Not Present");
        } catch (Exception e) {
            log.error("[JwtFilter] Failed to resolve token for path {}: {}", path, e.getMessage(), e);
        }

        if (token != null && !token.isEmpty()) {
            try {
                JwtStatus validationResult = jwtTokenProvider.validateToken(token);
                log.info("[JwtFilter] Token validation result: {}", validationResult);

                if (validationResult == JwtStatus.ACCESS) {
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    TokenDTO tokenInfo = jwtTokenProvider.getTokenInfo(token);
                    String userId = tokenInfo.userId() == null ? "" : tokenInfo.userId();
                    String roles = tokenInfo.role() == null ? "" : tokenInfo.role();

                    log.info("[JwtFilter] Authentication successful. User: {}, Roles: {}", authentication.getName(), authentication.getAuthorities());

                    // 인증된 사용자 정보를 헤더에 담아 요청 변형 (Downstream 서비스 전달용)
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header(HttpHeaders.AUTHORIZATION, JwtTokenProvider.HEADER_PREFIX + token)
                            .header("X-User-Id", userId)
                            .header("X-Authorities", roles)
                            .header("X-Gateway-Secret", gatewayTrustedSecret)
                            .build();
                    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

                    // Security Context에 Authentication 객체를 설정하고 필터 체인 계속 진행
                    log.info("[JwtFilter] Setting Authentication in context and continuing chain.");
                    return chain.filter(mutatedExchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                } else {
                    log.warn("[JwtFilter] Token is invalid or not an ACCESS token. Status: {}. Continuing chain without authentication.", validationResult);
                }
            } catch (Exception ex) {
                log.error("[JwtFilter] Token validation/authentication error for path {}: {}", path, ex.getMessage(), ex);
            }
        } else {
            log.info("[JwtFilter] No token present. Continuing chain without authentication.");
        }

        log.warn("[JwtFilter] Authentication not set. Continuing chain for authorization check by Spring Security.");
        return chain.filter(exchange);
    }
}
