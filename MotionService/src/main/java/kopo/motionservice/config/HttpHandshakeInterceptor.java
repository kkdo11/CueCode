package kopo.motionservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Base64;
import java.util.Map;

@Slf4j
public class HttpHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        String userId = null;

        // 1. 먼저 쿼리 파라미터에서 토큰 확인
        URI uri = request.getURI();
        String query = uri.getQuery();

        if (query != null && query.contains("token=")) {
            try {
                String token = extractTokenFromQuery(query);
                if (token != null && !token.isEmpty()) {
                    userId = extractUserIdFromToken(token);
                    log.info("[Handshake] Extracted User-Id from query parameter token: {}", userId);
                }
            } catch (Exception e) {
                log.warn("[Handshake] Failed to extract userId from token: {}", e.getMessage());
            }
        }

        // 2. 토큰에서 추출 실패 시 SecurityContext에서 시도
        if (userId == null || "anonymousUser".equals(userId)) {
            if (request instanceof ServletServerHttpRequest) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    userId = authentication.getName();
                    log.info("[Handshake] Intercepted User-Id from SecurityContext: {}", userId);
                } else {
                    log.info("[Handshake] No authenticated user found in SecurityContext");
                }
            }
        }

        // 3. userId를 attributes에 저장
        if (userId != null && !userId.isEmpty() && !"anonymousUser".equals(userId)) {
            attributes.put("userId", userId);
            log.info("[Handshake] Final User-Id set in attributes: {}", userId);
        } else {
            log.warn("[Handshake] No valid userId found, connection will be anonymous");
        }

        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, Exception exception) {
        // 핸드셰이크 이후 로직 (필요 시 구현)
    }

    /**
     * 쿼리 문자열에서 token 파라미터 값 추출
     */
    private String extractTokenFromQuery(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                return java.net.URLDecoder.decode(keyValue[1], java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * JWT 토큰에서 userId(sub) 추출 (서명 검증 없음)
     * 프로덕션 환경에서는 서명 검증을 추가해야 함
     */
    private String extractUserIdFromToken(String token) {
        try {
            // JWT 형식: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                log.warn("[Handshake] Invalid JWT token format");
                return null;
            }

            // payload 부분 디코딩
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            log.debug("[Handshake] Decoded JWT payload: {}", payload);

            // JSON에서 "sub" 필드 추출 (간단한 파싱)
            int subIndex = payload.indexOf("\"sub\"");
            if (subIndex >= 0) {
                int startQuote = payload.indexOf("\"", subIndex + 6);
                int endQuote = payload.indexOf("\"", startQuote + 1);
                if (startQuote >= 0 && endQuote > startQuote) {
                    String userId = payload.substring(startQuote + 1, endQuote);
                    log.info("[Handshake] Extracted userId from token: {}", userId);
                    return userId;
                }
            }

            log.warn("[Handshake] 'sub' field not found in JWT payload");
            return null;
        } catch (Exception e) {
            log.error("[Handshake] Error parsing JWT token", e);
            return null;
        }
    }
}
