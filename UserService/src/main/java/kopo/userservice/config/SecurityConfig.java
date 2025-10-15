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
}