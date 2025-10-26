package kopo.motionservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.filter.ForwardedHeaderFilter; // 추가된 import

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public TrustedHeaderAuthenticationFilter trustedHeaderAuthenticationFilter(
            @Value("${gateway.trusted.secret}") String gatewayTrustedSecret) {
        return new TrustedHeaderAuthenticationFilter(gatewayTrustedSecret);
    }

    /**
     * 1순위: 공개 엔드포인트 (인증 불필요)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicEndpointsFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/actuator/**",
                        "/health",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/ws/motion",
                        "/ws/alerts",
                        "/error"  // ✅ 추가: Spring Boot 기본 에러 페이지
                )
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }

    /**
     * 2순위: 보호된 API 엔드포인트 (인증 필요)
     */
    @Bean
    @Order(2)
    public SecurityFilterChain protectedApiFilterChain(
            HttpSecurity http,
            TrustedHeaderAuthenticationFilter filter) throws Exception {
        http
                .securityMatcher("/**")  // 명시적으로 모든 경로 지정
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                // ✅ 수정: addFilterBefore 사용
                .addFilterBefore(filter, org.springframework.security.web.access.intercept.AuthorizationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/motions/upload").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/v1/recorded-motions/**").hasAnyAuthority("ROLE_USER") // ✅ 명시적 경로
                        .requestMatchers("/motions/**").hasAnyAuthority("ROLE_USER", "ROLE_USER_MANAGER")
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}