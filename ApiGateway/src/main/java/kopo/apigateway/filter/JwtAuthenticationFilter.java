//package kopo.apigateway.filter;
//
//import kopo.apigateway.jwt.JwtStatus;
//import kopo.apigateway.jwt.JwtTokenProvider;
//import kopo.apigateway.jwt.JwtTokenType;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.ReactiveSecurityContextHolder;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebFilter;
//import org.springframework.web.server.WebFilterChain;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class JwtAuthenticationFilter implements WebFilter {
//
//    private final JwtTokenProvider jwtTokenProvider;
//
//    // Define paths to be skipped from JWT authentication
//    private final List<String> skipPaths = List.of(
//            "/login",
//            "/reg",
//            "/actuator"
//    );
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
//        ServerHttpRequest request = exchange.getRequest();
//        String path = request.getURI().getPath();
//
//        // Skip JWT validation for defined paths
//        if (skipPaths.stream().anyMatch(path::startsWith)) {
//            return chain.filter(exchange);
//        }
//
//        String token = jwtTokenProvider.resolveToken(request, JwtTokenType.ACCESS_TOKEN);
//
//        // If token exists and is valid, set authentication in context
//        if (token != null && jwtTokenProvider.validateToken(token) == JwtStatus.ACCESS) {
//            Authentication authentication = jwtTokenProvider.getAuthentication(token);
//            log.info("Authenticated user: {}, roles: {}", authentication.getName(), authentication.getAuthorities());
//
//            // Mutate request to forward Authorization header to downstream services
//            ServerHttpRequest mutatedRequest = request.mutate()
//                    .header(HttpHeaders.AUTHORIZATION, JwtTokenProvider.HEADER_PREFIX + token)
//                    .build();
//            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
//
//            return chain.filter(mutatedExchange)
//                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
//        }
//
//        // For other cases, proceed without authentication
//        log.debug("JWT Token is invalid or not present for path: {}", path);
//        return chain.filter(exchange);
//    }
//}
// language: java
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

    private final List<String> skipPaths = List.of(
            "/login",
            "/reg",
            "/actuator"
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
            if (jwtTokenProvider.validateToken(token) == JwtStatus.ACCESS) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                TokenDTO tokenInfo = jwtTokenProvider.getTokenInfo(token);
                String userId = tokenInfo.userId() == null ? "" : tokenInfo.userId();
                String roles = tokenInfo.role() == null ? "" : tokenInfo.role();

                log.debug("Authenticated user: {}, roles: {}", authentication.getName(), authentication.getAuthorities());

                ServerHttpRequest mutatedRequest = request.mutate()
                        .header(HttpHeaders.AUTHORIZATION, JwtTokenProvider.HEADER_PREFIX + token)
                        .header("X-User-Id", userId)
                        .header("X-Authorities", roles)
                        .header("X-Gateway-Secret", gatewayTrustedSecret)
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
