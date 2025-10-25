package kopo.apigateway.config;

import kopo.apigateway.filter.JwtAuthenticationFilter;
import kopo.apigateway.hadler.AccessDeniedHandler;
import kopo.apigateway.hadler.LoginServerAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebFluxSecurity
public class SecurityConfig {

    private final AccessDeniedHandler accessDeniedHandler;
    private final LoginServerAuthenticationEntryPoint loginServerAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        log.info(this.getClass().getName() + ".filterChain Start!");
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        http.formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        http.exceptionHandling(e -> e.accessDeniedHandler(accessDeniedHandler));
        http.exceptionHandling(e -> e.authenticationEntryPoint(loginServerAuthenticationEntryPoint));
        http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        http.authorizeExchange(authz -> authz
                .pathMatchers(
                        // 🚨 인증 없이 접근 허용 (permitAll)
                        "/api/user/reg/**",          // 회원가입 관련
                        "/api/login/**",             // 로그인 관련
                        "/api/reg/**",// 회원가입 관련
                        "/actuator/**",
                        "/api/user/actuator/**",     // User Service 액추에이터
                        "/api/actuator/**",          // 게이트웨이 자체 액추에이터
                        "/api/swagger-ui/**", "/api/v3/api-docs/**", // API 문서
                        "/api/user/me",               // 로그인 상태 확인용 (인증 필수는 아님)
                        "/api/user/v1/logout"
                ).permitAll()
                // 👨‍👩‍👧‍👦 관리자(보호자)만 접근 허용 (ROLE_USER_MANAGER)
                .pathMatchers(
                        "/api/user/dashboard",
                        "/api/patient/list",
                        "/api/manager/addPatient",
                        "/api/patient/**"
                ).hasAuthority("ROLE_USER_MANAGER")

                // 🧑‍⚕️ 환자만 접근 허용 (ROLE_USER)
                .pathMatchers(
                        "/api/patient/dashboard.html",
                        "/api/patient/detection-area/update",
                        "/api/motions/upload"
                ).hasAuthority("ROLE_USER")

                // 🤝 환자 또는 관리자 모두 접근 허용 (ROLE_USER, ROLE_USER_MANAGER)
                .pathMatchers(
                        "/api/user/info",
                        "/api/user/verify-password",
                        "/api/user/update-name",
                        "/api/user/update-email",
                        "/api/user/detection-area",
                        "/api/user/update-detection-area",
                        "/api/users/contact",
                        "/api/motions/alerts",
                        "/api/user/update-password"
                ).hasAnyAuthority("ROLE_USER", "ROLE_USER_MANAGER")

                // ⚠️ '/api/user/' 하위 경로 중 위에 명시되지 않은 나머지 경로는 환자만 접근
                .pathMatchers("/api/user/**").hasAuthority("ROLE_USER")

                // 🚫 명시적으로 허용/권한 부여되지 않은 모든 요청은 차단 (denyAll)
                .anyExchange().denyAll()
        );
        http.addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.HTTP_BASIC);
        log.info(this.getClass().getName() + ".filterChain End!");
        return http.build();
    }

    /**
     * CORS 설정을 위한 Bean (프론트 서버에서 API 호출 가능하도록 허용)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 프론트 개발 서버(예: python -m http.server 8080)에서 호출할 수 있도록 8080 origin 추가
        config.setAllowedOrigins(List.of("http://localhost:14000","http://localhost:13000","http://localhost:8080"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
