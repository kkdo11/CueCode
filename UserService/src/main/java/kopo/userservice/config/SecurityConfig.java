package kopo.userservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;


import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${jwt.secret.key}")
    private String secretKeyBase64;
    @Value("${jwt.token.access.name}")
    private String accessTokenName;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    // --- [추가] 1. 공개 경로용 보안 필터 체인 ---
    // 가장 먼저 실행되도록 @Order(1) 설정
    @Bean
    @Order(1)
    public SecurityFilterChain publicEndpointsFilterChain(HttpSecurity http) throws Exception {
        http
                // 이 필터 체인이 처리할 경로를 명시적으로 지정
                .securityMatcher(
                        "/", "/index.html", "/css/**", "/js/**", "/images/**",
                        "/auth/login", "/auth/refresh", "/health", "/login",
                        "/swagger-ui/**", "/v3/api-docs/**",
                        "/actuator/**" // actuator 경로 포함
                )
                // 위 경로들에 대해서는 모든 보안 기능을 비활성화하고 모두 허용
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(fl -> fl.disable())
                .httpBasic(b -> b.disable());

        return http.build();
    }

    // --- [수정] 2. JWT 인증이 필요한 API용 보안 필터 체인 ---
    // 공개 경로용 필터 다음에 실행되도록 @Order(2) 설정
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                // 이 필터 체인은 위에서 지정한 공개 경로를 제외한 모든 경로를 처리
                .securityMatcher("/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 위에서 이미 처리된 공개 경로는 여기서 다시 설정할 필요 없음
                        // 모든 요청은 인증을 요구하도록 간단하게 설정
                        .anyRequest().authenticated()
                )
                // JWT 검증 로직은 이 필터 체인에만 적용
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(cookieFirstBearerTokenResolver())
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secret = Base64.getDecoder().decode(secretKeyBase64);
        SecretKey key = new SecretKeySpec(secret, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public BearerTokenResolver cookieFirstBearerTokenResolver() {
        return request -> {
            var cookies = request.getCookies();
            if (cookies != null) {
                for (var c : cookies) {
                    if (accessTokenName.equals(c.getName())) {
                        var v = c.getValue();
                        if (v != null && !v.isBlank()) return v;
                    }
                }
            }
            return new DefaultBearerTokenResolver().resolve(request);
        };
    }
}/*
package kopo.userservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // HttpMethod 임포트
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    // ✅ Config Server 값 주입
    @Value("${jwt.secret.key}")          private String secretKeyBase64;
    @Value("${jwt.token.access.name}")   private String accessTokenName;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(fl -> fl.disable())
                .httpBasic(b -> b.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html",
                                "/css/**", "/js/**", "/images/**",
                                "/auth/login", "/auth/refresh", "/health",
                                "/login", // 로그인 API
                                "/swagger-ui/**", // Swagger UI
                                "/v3/api-docs/**", // Swagger API docs
                                "/actuator/**" // actuator
                                , "/user/**" // 정적 리소스 및 회원가입 페이지
                                , "/users/**" // 메서드 레벨 보안을 위해 일단 허용
                                , "/patient/**"
                                , "/manager/**"
                                , "/patient/detection-area" // 감지 범위 조회/변경 허용
                                , "/user/info" // 사용자 정보 조회 허용
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/reg/**").permitAll() // 아이디 중복확인 등 GET도 허용
                        .requestMatchers(HttpMethod.POST, "/reg/**").permitAll() // 기존 코드 유지
                        .anyRequest().authenticated()
                )
                // ✅ 커스텀 필터 없이 JWT 검증
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(cookieFirstBearerTokenResolver()) // 쿠키 우선
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))                 // HS256 검증
                );

        return http.build();
    }

    // ✅ HS256 Decoder (알고리즘 명시 없이 버전 호환)
    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secret = Base64.getDecoder().decode(secretKeyBase64);
        SecretKey key = new SecretKeySpec(secret, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    // ✅ 쿠키 → 없으면 Authorization 헤더
    @Bean
    public BearerTokenResolver cookieFirstBearerTokenResolver() {
        return request -> {
            var cookies = request.getCookies();
            if (cookies != null) {
                for (var c : cookies) {
                    if (accessTokenName.equals(c.getName())) {
                        var v = c.getValue();
                        if (v != null && !v.isBlank()) return v;
                    }
                }
            }
            return new DefaultBearerTokenResolver().resolve(request);
        };
    }
}*/
