package kopo.motionservice.config;

import com.sun.security.auth.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String userId = (String) attributes.get("userId");

        if (userId == null || userId.isBlank()) {
            // userId가 없는 경우, 익명 사용자로 처리하거나 고유 ID를 부여할 수 있습니다.
            // 여기서는 임시로 UUID를 사용합니다.
            userId = "anonymous-" + UUID.randomUUID().toString();
        }

        return new UserPrincipal(userId);
    }
}

