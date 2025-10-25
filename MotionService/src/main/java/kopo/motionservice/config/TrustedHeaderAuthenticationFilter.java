//package kopo.motionservice.config; // Ensure this package matches your project structure
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.Collections; // Import Collections for emptyList
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * Filter that restores authentication using X-User-Id / X-Authorities headers sent by the gateway.
// * Verifies the trustworthiness of the request via the gateway secret key (X-Gateway-Secret).
// * Includes detailed logging for debugging purposes.
// */
//public class TrustedHeaderAuthenticationFilter extends OncePerRequestFilter {
//
//    private static final Logger log = LoggerFactory.getLogger(TrustedHeaderAuthenticationFilter.class);
//
//    // Secret key injected from Spring SecurityConfig via @Value("${gateway.trusted.secret}")
//    private final String gatewayTrustedSecret;
//    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";
//    private static final String USER_ID_HEADER = "X-User-Id";
//    private static final String AUTHORITIES_HEADER = "X-Authorities";
//
//
//    /**
//     * Filter constructor: Injects the secret key set via @Value in SecurityConfig.
//     * @param gatewayTrustedSecret Shared secret key defined in the application configuration file.
//     */
//    public TrustedHeaderAuthenticationFilter(String gatewayTrustedSecret) {
//        // Initialize to prevent null or blank strings, ensuring comparability
//        this.gatewayTrustedSecret = (gatewayTrustedSecret == null) ? "" : gatewayTrustedSecret.trim();
//        log.info("✅ TrustedHeaderAuthenticationFilter initialized.");
//        if (this.gatewayTrustedSecret.isBlank()) {
//            // Log a strong warning if the secret key is not configured
//            log.warn("🚨 [SECURITY WARNING] Gateway trusted secret ('gateway.trusted.secret') is BLANK or not configured. " +
//                    "The service is VULNERABLE as it cannot verify requests from the gateway!");
//        } else {
//            // Log confirmation that the secret key is set
//            log.info("🔑 Gateway trusted secret is configured. Length: {} characters.", this.gatewayTrustedSecret.length());
//        }
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//
//        // --- 💡 [Added Debug Logging] ---
//        log.info("==================== TrustedHeaderAuthenticationFilter - START ====================");
//        log.info("➡️ Request URI: {} {}", request.getMethod(), request.getRequestURI());
//
//        // 1. Verify Gateway Trustworthiness: Check X-Gateway-Secret header
//        String receivedSecret = request.getHeader(GATEWAY_SECRET_HEADER);
//
//        // Clearly log the expected value (from config) and the received value (from header)
//        log.info("🔑 Expected Secret (from motion-dev.yml): '{}' (Length: {})",
//                this.gatewayTrustedSecret, this.gatewayTrustedSecret.length());
//        log.info("🔑 Received Secret (from {} Header): '{}' (Length: {})",
//                GATEWAY_SECRET_HEADER,
//                receivedSecret, (receivedSecret != null ? receivedSecret.length() : "null"));
//
//        // Explicitly perform the comparison and log the result
//        boolean isSecretMatch = this.gatewayTrustedSecret.equals(receivedSecret);
//        log.info("🔑 Secret Key Match Result: {}", isSecretMatch ? "✅ SUCCESS" : "❌ FAILED");
//
//        // If the secret key is configured (not blank) AND the received key does not match
//        if (!this.gatewayTrustedSecret.isBlank() && !isSecretMatch) {
//            log.warn("🚨 UNAUTHORIZED ACCESS BLOCKED: Mismatched or missing trusted secret header ({}). Expected '{}' but received '{}'. Responding with 401.",
//                    GATEWAY_SECRET_HEADER, this.gatewayTrustedSecret, receivedSecret);
//            log.info("==================== TrustedHeaderAuthenticationFilter - END (BLOCKED) ====================");
//
//            // Return 401 UNAUTHORIZED
//            response.setStatus(HttpStatus.UNAUTHORIZED.value());
//            response.getWriter().write("Invalid or Missing Gateway Secret"); // Send a clear error message
//            return; // Stop filter chain execution
//        } else if (this.gatewayTrustedSecret.isBlank()) {
//            log.warn("🚨 Proceeding WITHOUT secret validation because 'gateway.trusted.secret' is not set in configuration.");
//        } else {
//            log.info("✅ Gateway trust validated successfully.");
//        }
//
//
//        // 2. Extract and Register Authentication Information (only if trust is verified or not required)
//        String userId = request.getHeader(USER_ID_HEADER);
//        String authoritiesHeader = request.getHeader(AUTHORITIES_HEADER);
//
//        log.info("👤 Received {}: '{}'", USER_ID_HEADER, userId);
//        log.info("🛡️ Received {}: '{}'", AUTHORITIES_HEADER, authoritiesHeader);
//
//        // Proceed with authentication restoration only if userId is present
//        if (userId != null && !userId.isBlank()) {
//            List<SimpleGrantedAuthority> authorities = Collections.emptyList(); // Default to empty list
//
//            // Parse Authorities (convert comma-separated string to List<SimpleGrantedAuthority>)
//            if (authoritiesHeader != null && !authoritiesHeader.isBlank()) {
//                authorities = Arrays.stream(authoritiesHeader.split(","))
//                        .map(String::trim) // Remove leading/trailing whitespace
//                        .filter(s -> !s.isEmpty()) // Filter out empty strings
//                        .map(SimpleGrantedAuthority::new) // Create authority objects
//                        .collect(Collectors.toList());
//            } else {
//                log.warn("❓ {} header was present, but {} header was missing or empty. User will have no authorities.", USER_ID_HEADER, AUTHORITIES_HEADER);
//            }
//
//            // Register authentication information in Spring Security Context
//            // The principal is the userId, credentials are null (not needed), and authorities list
//            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//            log.info("✅ Authentication context successfully set for user: '{}', Granted Authorities: {}", userId, authorities);
//
//        } else if (!this.gatewayTrustedSecret.isBlank()) {
//            // If trust was verified but X-User-Id is missing, log a debug message (might be expected for some endpoints)
//            log.debug("❓ Trusted request received, but {} header was missing or blank. Proceeding without setting authentication context.", USER_ID_HEADER);
//        } else if (userId == null || userId.isBlank()){
//            // If secret is blank AND userId is missing
//            log.debug("❓ No secret validation required and {} header missing. Proceeding without authentication.", USER_ID_HEADER);
//        }
//
//        log.info("==================== TrustedHeaderAuthenticationFilter - END (PASSED) =====================");
//
//        // Continue the filter chain
//        filterChain.doFilter(request, response);
//    }
//}

package kopo.motionservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Restores authentication from X-User-Id / X-Authorities that the gateway forwards.
 * Optionally validates trust via X-Gateway-Secret, but SKIPS for public endpoints
 * (including WebSocket/SSE) and CORS preflights.
 */
public class TrustedHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TrustedHeaderAuthenticationFilter.class);

    private final String gatewayTrustedSecret;

    private static final String HDR_GATEWAY_SECRET = "X-Gateway-Secret";
    private static final String HDR_USER_ID       = "X-User-Id";
    private static final String HDR_AUTHORITIES   = "X-Authorities";

    // Public endpoints that should not require the secret (align with SecurityConfig)
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/actuator/", "/swagger-ui/", "/v3/api-docs/", "/ws/"
    );
    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            "/health", "/error"
    );

    public TrustedHeaderAuthenticationFilter(String gatewayTrustedSecret) {
        this.gatewayTrustedSecret = gatewayTrustedSecret == null ? "" : gatewayTrustedSecret.trim();
        log.info("TrustedHeaderAuthenticationFilter initialized. Secret configured: {}", this.gatewayTrustedSecret.isBlank() ? "NO" : "YES");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        final String uri    = req.getRequestURI();
        final String method = req.getMethod();

        // 0) Always skip CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            if (log.isDebugEnabled()) log.debug("Skipping OPTIONS preflight for {}", uri);
            chain.doFilter(req, res);
            return;
        }

        // 1) Skip for public endpoints (e.g., WebSocket /ws/** cannot send custom headers from browsers)
        if (isPublic(uri)) {
            if (log.isDebugEnabled()) log.debug("Public endpoint detected ({}). Skipping secret validation.", uri);
            // Still attempt to restore authentication if headers are present (useful for server-to-server)
            restoreAuthenticationIfPresent(req);
            chain.doFilter(req, res);
            return;
        }

        // 2) Protected endpoints: validate secret IFF a secret is configured
        if (!gatewayTrustedSecret.isBlank()) {
            final String received = req.getHeader(HDR_GATEWAY_SECRET);
            final boolean match = gatewayTrustedSecret.equals(received);

            if (!match) {
                log.warn("Blocked: missing/mismatched {} for protected URI {}. Responding 401.", HDR_GATEWAY_SECRET, uri);
                res.setStatus(HttpStatus.UNAUTHORIZED.value());
                res.getWriter().write("Invalid or Missing Gateway Secret");
                return;
            }
            if (log.isDebugEnabled()) log.debug("Gateway secret validated for {}", uri);
        } else {
            log.warn("Proceeding WITHOUT secret validation on protected URI {} because secret is not configured.", uri);
        }

        // 3) Restore authentication (if gateway forwarded user headers)
        restoreAuthenticationIfPresent(req);

        chain.doFilter(req, res);
    }

    private boolean isPublic(String uri) {
        if (PUBLIC_EXACT_PATHS.contains(uri)) return true;
        for (String p : PUBLIC_PATH_PREFIXES) {
            if (uri.startsWith(p)) return true;
        }
        return false;
    }

    private void restoreAuthenticationIfPresent(HttpServletRequest req) {
        String userId = req.getHeader(HDR_USER_ID);
        String authsHeader = req.getHeader(HDR_AUTHORITIES);

        if (userId == null || userId.isBlank()) {
            if (log.isTraceEnabled()) log.trace("No {} header; leaving SecurityContext unauthenticated.", HDR_USER_ID);
            return;
        }

        List<SimpleGrantedAuthority> authorities = Collections.emptyList();
        if (authsHeader != null && !authsHeader.isBlank()) {
            authorities = Arrays.stream(authsHeader.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        if (log.isDebugEnabled()) {
            log.debug("Restored authentication from headers for user='{}', authorities={}", userId, authorities);
        }
    }
}