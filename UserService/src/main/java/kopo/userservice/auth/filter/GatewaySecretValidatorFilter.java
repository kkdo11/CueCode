package kopo.userservice.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewaySecretValidatorFilter extends OncePerRequestFilter {

    @Value("${gateway.trusted.secret}")
    private String gatewaySecret;

    private static final String GATEWAY_SECRET_HEADER_NAME = "X-Gateway-Secret";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestSecret = request.getHeader(GATEWAY_SECRET_HEADER_NAME);

        if (requestSecret == null || !requestSecret.equals(gatewaySecret)) {
            log.warn("Gateway secret mismatch or missing. Request from {} rejected.", request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Denied: Invalid or missing gateway secret.");
            return;
        }

        log.info("Gateway secret validated successfully.");
        filterChain.doFilter(request, response);
    }
}
