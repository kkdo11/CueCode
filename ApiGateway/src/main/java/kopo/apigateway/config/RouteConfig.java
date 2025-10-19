package kopo.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Slf4j
@Configuration
public class RouteConfig {

    // Inject URIs from application.yml
    @Value("${service.uri.user}")
    private String userServiceUri;

    @Value("${service.uri.front}")
    private String frontUiServiceUri;

    @Value("${service.uri.motion}")
    private String motionServiceUri;

    /**
     * Gateway 라우팅: URI를 설정 파일에서 주입받아 동적으로 생성
     */
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // FrontUI가 제공하는 로그인 페이지로 GET 요청을 라우팅합니다.
                .route("front-ui-login-page", r -> r
                        .path("/login")
                        .and()
                        .method(HttpMethod.GET)
                        .uri(frontUiServiceUri))

                // FrontUI가 제공하는 대시보드 페이지로 GET 요청을 라우팅합니다.
                .route("front-ui-dashboard", r -> r
                        .path("/dashboard")
                        .and()
                        .method(HttpMethod.GET)
                        .uri(frontUiServiceUri))

                // /user/dashboard 경로도 프론트엔드로 라우팅 (정적 대시보드 페이지, 경로 리라이트)
                .route("front-ui-user-dashboard", r -> r
                        .path("/user/dashboard")
                        .and()
                        .method(HttpMethod.GET)
                        .filters(f -> f.rewritePath("/user/dashboard", "/user/dashboard.html"))
                        .uri(frontUiServiceUri))

                // Motion-detector 페이지 라우팅
                .route("front-ui-motion-detector", r -> r
                        .path("/motion")
                        .and()
                        .method(HttpMethod.GET)
                        .filters(f -> f.rewritePath("/motion", "/motion/motion-detector.html"))
                        .uri(frontUiServiceUri))

                // Motion-upload 페이지 라우팅
                .route("front-ui-motion-upload", r -> r
                        .path("/motion/upload")
                        .and()
                        .method(HttpMethod.GET)
                        .filters(f -> f.rewritePath("/motion/upload", "/motion/motion-upload.html"))
                        .uri(frontUiServiceUri))

                // Vendor static assets (e.g., FFmpeg)
                .route("front-ui-vendor-assets", r -> r
                        .path("/vendor/**")
                        .uri(frontUiServiceUri))

                // UserService가 처리하는 로그인 POST 요청을 라우팅합니다.
                .route("user-service-login-post", r -> r
                        .path("/login")
                        .and()
                        .method(HttpMethod.POST)
                        .uri(userServiceUri))

                // 기존의 유저 등록, 조회 관련 라우팅은 유지합니다.
                .route("user-service-user", r -> r
                        .path("/user/**")
                        .uri(userServiceUri))
                .route("user-service-reg", r -> r
                        .path("/reg/**")
                        .uri(userServiceUri))

                // MotionService 라우팅
                .route("motion-service", r -> r
                        .path("/motions/**")
                        .uri(motionServiceUri))
                // Patient-service 라우팅 추가
                .route("patient-service", r -> r
                        .path("/patient/**")
                        .uri(userServiceUri))
                // Manager-service 라우팅 추가
                .route("manager-service", r -> r
                        .path("/manager/**")
                        .uri(userServiceUri))

                .build();
    }
}
