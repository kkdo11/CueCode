////package kopo.apigateway.filter;
////
////import kopo.apigateway.jwt.JwtStatus;
////import kopo.apigateway.jwt.JwtTokenProvider;
////import kopo.apigateway.jwt.JwtTokenType;
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.http.HttpHeaders;
////import org.springframework.http.server.reactive.ServerHttpRequest;
////import org.springframework.security.core.Authentication;
////import org.springframework.security.core.context.ReactiveSecurityContextHolder;
////import org.springframework.stereotype.Component;
////import org.springframework.web.server.ServerWebExchange;
////import org.springframework.web.server.WebFilter;
////import org.springframework.web.server.WebFilterChain;
////import reactor.core.publisher.Mono;
////
////import java.util.List;
////
////@Slf4j
////@Component
////@RequiredArgsConstructor
////public class JwtAuthenticationFilter implements WebFilter {
////
////    private final JwtTokenProvider jwtTokenProvider;
////
////    // Define paths to be skipped from JWT authentication
////    private final List<String> skipPaths = List.of(
////            "/login",
////            "/reg",
////            "/actuator"
////    );
////
////    @Override
////    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
////        ServerHttpRequest request = exchange.getRequest();
////        String path = request.getURI().getPath();
////
////        // Skip JWT validation for defined paths
////        if (skipPaths.stream().anyMatch(path::startsWith)) {
////            return chain.filter(exchange);
////        }
////
////        String token = jwtTokenProvider.resolveToken(request, JwtTokenType.ACCESS_TOKEN);
////
////        // If token exists and is valid, set authentication in context
////        if (token != null && jwtTokenProvider.validateToken(token) == JwtStatus.ACCESS) {
////            Authentication authentication = jwtTokenProvider.getAuthentication(token);
////            log.info("Authenticated user: {}, roles: {}", authentication.getName(), authentication.getAuthorities());
////
////            // Mutate request to forward Authorization header to downstream services
////            ServerHttpRequest mutatedRequest = request.mutate()
////                    .header(HttpHeaders.AUTHORIZATION, JwtTokenProvider.HEADER_PREFIX + token)
////                    .build();
////            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
////
////            return chain.filter(mutatedExchange)
////                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
////        }
////
////        // For other cases, proceed without authentication
////        log.debug("JWT Token is invalid or not present for path: {}", path);
////        return chain.filter(exchange);
////    }
////}
//// language: java
//package kopo.apigateway.filter;
//
//import kopo.apigateway.jwt.JwtStatus;
//import kopo.apigateway.jwt.JwtTokenProvider;
//import kopo.apigateway.jwt.JwtTokenType;
//import kopo.apigateway.dto.TokenDTO;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
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
//    @Value("${gateway.trusted.secret:}")
//    private String gatewayTrustedSecret;
//
//    private final List<String> skipPaths = List.of(
//            "/login",
//            "/reg",
//            "/actuator",
//            "/api"
//    );
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
//        ServerHttpRequest request = exchange.getRequest();
//        String path = request.getURI().getPath();
//
//        if (skipPaths.stream().anyMatch(path::startsWith)) {
//            return chain.filter(exchange);
//        }
//
//        String token;
//        try {
//            token = jwtTokenProvider.resolveToken(request, JwtTokenType.ACCESS_TOKEN);
//        } catch (Exception e) {
//            log.debug("Failed to resolve token: {}", e.getMessage());
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }
//
//        if (token == null) {
//            log.debug("No token present for path: {}", path);
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }
//
//        try {
//            if (jwtTokenProvider.validateToken(token) == JwtStatus.ACCESS) {
//                Authentication authentication = jwtTokenProvider.getAuthentication(token);
//                TokenDTO tokenInfo = jwtTokenProvider.getTokenInfo(token);
//                String userId = tokenInfo.userId() == null ? "" : tokenInfo.userId();
//                String roles = tokenInfo.role() == null ? "" : tokenInfo.role();
//
//                log.debug("Authenticated user: {}, roles: {}", authentication.getName(), authentication.getAuthorities());
//
//                ServerHttpRequest mutatedRequest = request.mutate()
//                        .header(HttpHeaders.AUTHORIZATION, JwtTokenProvider.HEADER_PREFIX + token)
//                        .header("X-User-Id", userId)
//                        .header("X-Authorities", roles)
//                        .header("X-Gateway-Secret", gatewayTrustedSecret)
//                        .build();
//                ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
//
//                return chain.filter(mutatedExchange)
//                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
//            } else {
//                log.debug("Token invalid or not ACCESS for path: {}", path);
//                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//                return exchange.getResponse().setComplete();
//            }
//        } catch (Exception ex) {
//            log.debug("Token validation/authentication error: {}", ex.getMessage());
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }
//    }
//}

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

    // Define paths to be skipped from JWT authentication
    private final List<String> skipPaths = List.of(
            "/login",
            "/reg",
            "/actuator",
            "/api/login"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("--- JWT Filter Processing Request: {} ---", path);

        // 1. Check for skip paths
        if (skipPaths.stream().anyMatch(path::startsWith)) {
            log.info("Path matched skip list. Bypassing JWT validation for: {}", path);
            return chain.filter(exchange);
        }

        String token;
        try {
            // 2. Resolve token
            token = jwtTokenProvider.resolveToken(request, JwtTokenType.ACCESS_TOKEN);
            if (token == null) {
                log.warn("No Access Token found in request headers for path: {}. Rejecting with 401.", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        } catch (Exception e) {
            // 2.1. Token resolution failure
            log.warn("Failed to resolve token for path: {}. Error: {}. Rejecting with 401.", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            // 3. Validate token
            JwtStatus status = jwtTokenProvider.validateToken(token);

            if (status == JwtStatus.ACCESS) {
                // 4. Token is valid (ACCESS)
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                TokenDTO tokenInfo = jwtTokenProvider.getTokenInfo(token);
                String userId = tokenInfo.userId() == null ? "N/A" : tokenInfo.userId();
                String roles = tokenInfo.role() == null ? "N/A" : tokenInfo.role();

                log.info("JWT ACCESS granted for User: {} (ID: {}). Forwarding request and setting context.", authentication.getName(), userId);
                log.debug("Roles: {}, Gateway Secret Present: {}", roles, !gatewayTrustedSecret.isEmpty());

                // Mutate request to forward authentication details via headers
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
                // 5. Token invalid (EXPIRED, DENY, etc.)
                log.warn("Token validation failed for path: {}. Status: {}. Rejecting with 401.", path, status);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        } catch (Exception ex) {
            // 6. General validation/authentication error
            log.error("Critical Token processing error for path: {}. Exception: {}", path, ex.getMessage(), ex);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
