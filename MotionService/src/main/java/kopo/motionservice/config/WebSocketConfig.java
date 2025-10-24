package kopo.motionservice.config;

import kopo.motionservice.handler.MotionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final MotionHandler motionHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(motionHandler, "/ws/motion")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpHandshakeInterceptor())
                .setHandshakeHandler(new CustomHandshakeHandler());
    }
}
