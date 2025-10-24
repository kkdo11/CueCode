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

        // skipPaths에 해당하는 경로는 JWT 인증 필터를 건너뜀
        if (skipPaths.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String token = null;
        try {
            token = jwtTokenProvider.resolveToken(request, JwtTokenType.ACCESS_TOKEN);
        } catch (Exception e) {
            log.debug("Failed to resolve token for path {}: {}", path, e.getMessage());
            // 토큰 추출 실패 시에도 응답을 완료하지 않고 체인 계속 진행
            // Spring Security의 authorizeExchange가 처리하도록 함
        }

        if (token != null) {
            try {
                if (jwtTokenProvider.validateToken(token) == JwtStatus.ACCESS) {
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    TokenDTO tokenInfo = jwtTokenProvider.getTokenInfo(token);
                    String userId = tokenInfo.userId() == null ? "" : tokenInfo.userId();
                    String roles = tokenInfo.role() == null ? "" : tokenInfo.role();

                    log.debug("Authenticated user: {}, roles: {} for path: {}", authentication.getName(),
                            authentication.getAuthorities(), path);

                    // 인증된 사용자 정보를 헤더에 담아 요청 변형 (Downstream 서비스 전달용)
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header(HttpHeaders.AUTHORIZATION, JwtTokenProvider.HEADER_PREFIX + token)
                            .header("X-User-Id", userId)
                            .header("X-Authorities", roles)
                            .header("X-Gateway-Secret", gatewayTrustedSecret)
                            .build();
                    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

                    // Security Context에 Authentication 객체를 설정하고 필터 체인 계속 진행
                    return chain.filter(mutatedExchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                } else {
                    log.debug("Token invalid or not ACCESS for path: {}. Continuing chain.", path);
                    // 토큰 유효성 검증 실패 시에도 응답을 완료하지 않고 체인 계속 진행
                }
            } catch (Exception ex) {
                log.debug("Token validation/authentication error for path {}: {}", path, ex.getMessage());
                // 토큰 처리 중 예외 발생 시에도 응답을 완료하지 않고 체인 계속 진행
            }
        } else {
            log.debug("No token present for path: {}. Continuing chain.", path);
            // 토큰이 없는 경우에도 응답을 완료하지 않고 체인 계속 진행
        }

        // 토큰이 없거나 유효하지 않거나 처리 중 오류가 발생했더라도,
        // 필터 체인을 계속 진행하여 Spring Security의 authorizeExchange가 처리하도록 함.
        // authorizeExchange는 Authentication 객체가 없으면 authenticationEntryPoint(401)를 호출할 것임.
        return chain.filter(exchange);
    }
}
