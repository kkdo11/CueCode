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
                        "/user/reg/**",      // 회원가입
                        "/login/**",
                        "/reg/**",
                        "/user/actuator/**", // ✅ 게이트웨이 경유 액추에이터
                        "/actuator/**",
                        "/swagger-ui/**", "/v3/api-docs/**"
                ).permitAll()
                .pathMatchers("/user/dashboard").hasAuthority("ROLE_USER_MANAGER") // 보호자만 접근
                .pathMatchers("/patient/list").hasAuthority("ROLE_USER_MANAGER") // 보호자만 접근
                .pathMatchers("/patient/dashboard.html").hasAuthority("ROLE_USER") // 환자만 접근
                .pathMatchers("/user/me").hasAuthority( "ROLE_USER") // 환자 모두 접근
                .pathMatchers("/patient/detection-area/update").hasAuthority("ROLE_USER") // 보호자와 환자 모두 접근
                .pathMatchers("/user/info").hasAnyAuthority("ROLE_USER", "ROLE_USER_MANAGER") // 환자+관리자 정보조회
                .pathMatchers("/user/verify-password").hasAnyAuthority("ROLE_USER", "ROLE_USER_MANAGER") // 환자+관리자 본인확인
                        .pathMatchers("/user/update-name").hasAnyAuthority("ROLE_USER", "ROLE_USER_MANAGER")
                .pathMatchers("/user/**").hasAuthority("ROLE_USER")         // 환자만 접근


                .anyExchange().permitAll()
//                .anyExchange().denyAll()

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
        config.setAllowedOrigins(List.of("http://localhost:14000","http://localhost:13000"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
