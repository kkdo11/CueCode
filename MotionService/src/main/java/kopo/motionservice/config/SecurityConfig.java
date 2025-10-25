package kopo.motionservice.config;

import kopo.motionservice.security.TrustedHeaderAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public TrustedHeaderAuthenticationFilter trustedHeaderAuthenticationFilter(@Value("${gateway.trusted.secret:}") String gatewayTrustedSecret) {
        return new TrustedHeaderAuthenticationFilter(gatewayTrustedSecret);
    }

    /**
     * @Order(1) - 1순위 필터 체인: 인증이 필요 없는 공개 엔드포인트
     * /actuator/**, /swagger-ui/** 등 헬스 체크와 API 문서는 이 체인에서 처리됩니다.
     * 여기에는 TrustedHeaderAuthenticationFilter가 등록되지 않습니다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicEndpointsFilterChain(HttpSecurity http) throws Exception {
        http
                // 1순위 체인이 처리할 경로들만 명시적으로 지정
                .securityMatcher("/actuator/**", "/health", "/swagger-ui/**", "/v3/api-docs/**", "/ws/motion")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 위 경로들은 모두 허용
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }

    /**
     * @Order(2) - 2순위 필터 체인: 인증이 필요한 나머지 모든 API 엔드포인트
     * 1순위에서 처리되지 않은 모든 요청(/**)이 이 체인으로 넘어옵니다.
     * 여기에는 TrustedHeaderAuthenticationFilter가 등록되어 게이트웨이 인증을 검사합니다.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain protectedApiFilterChain(HttpSecurity http, TrustedHeaderAuthenticationFilter trustedHeaderAuthenticationFilter) throws Exception {
        http
                // 1순위에서 놓친 나머지 모든 경로(/**)를 처리
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 핵심: 인증 필터를 2순위 체인에만 등록
                .addFilterBefore(trustedHeaderAuthenticationFilter, org.springframework.security.web.context.SecurityContextPersistenceFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/motions/upload").hasAuthority("ROLE_USER")
                        .requestMatchers("/motions/alerts").hasAnyAuthority("ROLE_USER", "ROLE_USER_MANAGER")
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                );

        return http.build();
    }
}
