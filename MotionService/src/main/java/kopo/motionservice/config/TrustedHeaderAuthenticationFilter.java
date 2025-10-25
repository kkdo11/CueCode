package kopo.motionservice.config; // Ensure this package matches your project structure

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
import java.util.Arrays;
import java.util.Collections; // Import Collections for emptyList
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter that restores authentication using X-User-Id / X-Authorities headers sent by the gateway.
 * Verifies the trustworthiness of the request via the gateway secret key (X-Gateway-Secret).
 * Includes detailed logging for debugging purposes.
 */
public class TrustedHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TrustedHeaderAuthenticationFilter.class);

    // Secret key injected from Spring SecurityConfig via @Value("${gateway.trusted.secret}")
    private final String gatewayTrustedSecret;
    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String AUTHORITIES_HEADER = "X-Authorities";


    /**
     * Filter constructor: Injects the secret key set via @Value in SecurityConfig.
     * @param gatewayTrustedSecret Shared secret key defined in the application configuration file.
     */
    public TrustedHeaderAuthenticationFilter(String gatewayTrustedSecret) {
        // Initialize to prevent null or blank strings, ensuring comparability
        this.gatewayTrustedSecret = (gatewayTrustedSecret == null) ? "" : gatewayTrustedSecret.trim();
        log.info("✅ TrustedHeaderAuthenticationFilter initialized.");
        if (this.gatewayTrustedSecret.isBlank()) {
            // Log a strong warning if the secret key is not configured
            log.warn("🚨 [SECURITY WARNING] Gateway trusted secret ('gateway.trusted.secret') is BLANK or not configured. " +
                    "The service is VULNERABLE as it cannot verify requests from the gateway!");
        } else {
            // Log confirmation that the secret key is set
            log.info("🔑 Gateway trusted secret is configured. Length: {} characters.", this.gatewayTrustedSecret.length());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // --- 💡 [Added Debug Logging] ---
        log.info("==================== TrustedHeaderAuthenticationFilter - START ====================");
        log.info("➡️ Request URI: {} {}", request.getMethod(), request.getRequestURI());

        // 1. Verify Gateway Trustworthiness: Check X-Gateway-Secret header
        String receivedSecret = request.getHeader(GATEWAY_SECRET_HEADER);

        // Clearly log the expected value (from config) and the received value (from header)
        log.info("🔑 Expected Secret (from motion-dev.yml): '{}' (Length: {})",
                this.gatewayTrustedSecret, this.gatewayTrustedSecret.length());
        log.info("🔑 Received Secret (from {} Header): '{}' (Length: {})",
                GATEWAY_SECRET_HEADER,
                receivedSecret, (receivedSecret != null ? receivedSecret.length() : "null"));

        // Explicitly perform the comparison and log the result
        boolean isSecretMatch = this.gatewayTrustedSecret.equals(receivedSecret);
        log.info("🔑 Secret Key Match Result: {}", isSecretMatch ? "✅ SUCCESS" : "❌ FAILED");

        // If the secret key is configured (not blank) AND the received key does not match
        if (!this.gatewayTrustedSecret.isBlank() && !isSecretMatch) {
            log.warn("🚨 UNAUTHORIZED ACCESS BLOCKED: Mismatched or missing trusted secret header ({}). Expected '{}' but received '{}'. Responding with 401.",
                    GATEWAY_SECRET_HEADER, this.gatewayTrustedSecret, receivedSecret);
            log.info("==================== TrustedHeaderAuthenticationFilter - END (BLOCKED) ====================");

            // Return 401 UNAUTHORIZED
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid or Missing Gateway Secret"); // Send a clear error message
            return; // Stop filter chain execution
        } else if (this.gatewayTrustedSecret.isBlank()) {
            log.warn("🚨 Proceeding WITHOUT secret validation because 'gateway.trusted.secret' is not set in configuration.");
        } else {
            log.info("✅ Gateway trust validated successfully.");
        }


        // 2. Extract and Register Authentication Information (only if trust is verified or not required)
        String userId = request.getHeader(USER_ID_HEADER);
        String authoritiesHeader = request.getHeader(AUTHORITIES_HEADER);

        log.info("👤 Received {}: '{}'", USER_ID_HEADER, userId);
        log.info("🛡️ Received {}: '{}'", AUTHORITIES_HEADER, authoritiesHeader);

        // Proceed with authentication restoration only if userId is present
        if (userId != null && !userId.isBlank()) {
            List<SimpleGrantedAuthority> authorities = Collections.emptyList(); // Default to empty list

            // Parse Authorities (convert comma-separated string to List<SimpleGrantedAuthority>)
            if (authoritiesHeader != null && !authoritiesHeader.isBlank()) {
                authorities = Arrays.stream(authoritiesHeader.split(","))
                        .map(String::trim) // Remove leading/trailing whitespace
                        .filter(s -> !s.isEmpty()) // Filter out empty strings
                        .map(SimpleGrantedAuthority::new) // Create authority objects
                        .collect(Collectors.toList());
            } else {
                log.warn("❓ {} header was present, but {} header was missing or empty. User will have no authorities.", USER_ID_HEADER, AUTHORITIES_HEADER);
            }

            // Register authentication information in Spring Security Context
            // The principal is the userId, credentials are null (not needed), and authorities list
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("✅ Authentication context successfully set for user: '{}', Granted Authorities: {}", userId, authorities);

        } else if (!this.gatewayTrustedSecret.isBlank()) {
            // If trust was verified but X-User-Id is missing, log a debug message (might be expected for some endpoints)
            log.debug("❓ Trusted request received, but {} header was missing or blank. Proceeding without setting authentication context.", USER_ID_HEADER);
        } else if (userId == null || userId.isBlank()){
            // If secret is blank AND userId is missing
            log.debug("❓ No secret validation required and {} header missing. Proceeding without authentication.", USER_ID_HEADER);
        }

        log.info("==================== TrustedHeaderAuthenticationFilter - END (PASSED) =====================");

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
