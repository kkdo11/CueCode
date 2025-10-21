package kopo.motionservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 게이트웨이가 전달한 X-User-Id / X-Authorities 헤더를 이용해 인증을 복원하는 간단한 필터.
 * 주의: 내부 네트워크에서 게이트웨이를 신뢰할 수 있을 때만 사용하세요.
 */
public class TrustedHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TrustedHeaderAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        String authorities = request.getHeader("X-Authorities");

        if (userId != null && !userId.isBlank()) {
            List<SimpleGrantedAuthority> auths = List.of();
            if (authorities != null && !authorities.isBlank()) {
                auths = Arrays.stream(authorities.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, auths);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Restored authentication from headers: user={} authorities={}", userId, authorities);
        }

        filterChain.doFilter(request, response);
    }
}

