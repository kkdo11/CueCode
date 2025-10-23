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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // 게이트웨이의 비밀키를 주입받습니다. 이 키는 백엔드와 공유되어야 합니다.
    @Value("${gateway.trusted.secret:}")
    private String gatewayTrustedSecret;

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

        if (skipPaths.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String token;
        try {
            token = jwtTokenProvider.resolveToken(request, JwtTokenType.ACCESS_TOKEN);
        } catch (Exception e) {
            log.debug("Failed to resolve token: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        if (token == null) {
            log.debug("No token present for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            // If token exists and is valid, set authentication in context
            if (jwtTokenProvider.validateToken(token) == JwtStatus.ACCESS) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                TokenDTO tokenInfo = jwtTokenProvider.getTokenInfo(token);
                String userId = authentication.getName(); // 인증 객체에서 바로 ID 사용

                log.debug("Authenticated user: {}, roles: {}", authentication.getName(), authentication.getAuthorities());

                // 🚨🚨🚨 핵심 수정: X-Gateway-Secret 헤더 추가 🚨🚨🚨
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-Authorities", authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.joining(",")))
                        .header("X-Gateway-Secret", this.gatewayTrustedSecret) // <--- 이 부분이 추가됨
                        .build();

                ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

                return chain.filter(mutatedExchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            } else {
                log.debug("Token invalid or not ACCESS for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        } catch (Exception ex) {
            log.debug("Token validation/authentication error: {}", ex.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
