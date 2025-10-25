package kopo.motionservice.config;

import kopo.motionservice.security.TrustedHeaderAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * MotionService 시큐리티 설정: 게이트웨이가 전달한 X-User-Id/X-Authorities 헤더를
 * 복원하는 필터를 등록하고, /motions/upload 엔드포인트에 ROLE_USER 권한을 요구합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public TrustedHeaderAuthenticationFilter trustedHeaderAuthenticationFilter(@Value("${gateway.trusted.secret:}") String gatewayTrustedSecret) {
        return new TrustedHeaderAuthenticationFilter(gatewayTrustedSecret);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TrustedHeaderAuthenticationFilter trustedHeaderAuthenticationFilter) throws Exception {
        // 최신 Spring Security 6.1+ 스타일로 구성 (deprecated API 사용 회피)
        http.csrf(csrf -> csrf.disable());

        // Disable default HTTP Basic to avoid WWW-Authenticate header being sent on 401
        http.httpBasic(httpBasic -> httpBasic.disable());
        // Disable form login as well (not used)
        http.formLogin(form -> form.disable());

        // Return 401 without WWW-Authenticate by using HttpStatusEntryPoint
        http.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        // stateless (no session)
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // TrustedHeaderAuthenticationFilter를 SecurityContextPersistenceFilter 앞에 추가하여 순서를 명시
        http.addFilterBefore(trustedHeaderAuthenticationFilter, org.springframework.security.web.context.SecurityContextPersistenceFilter.class);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/motions/upload").hasAuthority("ROLE_USER")
                .requestMatchers("/actuator/**", "/health", "/swagger-ui/**", "/v3/api-docs/**", "/ws/motion").permitAll()
                .anyRequest().authenticated()
        );

        return http.build();
    }
}
