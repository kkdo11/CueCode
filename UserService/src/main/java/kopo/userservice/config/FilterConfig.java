package kopo.userservice.config;

import kopo.userservice.auth.filter.GatewaySecretValidatorFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    /**
     * GatewaySecretValidatorFilter가 서블릿 컨테이너에 자동으로 글로벌 필터로 등록되는 것을 방지합니다.
     * setEnabled(false)로 설정함으로써, 이 필터는 Spring Security의 SecurityFilterChain 내에서 수동으로 추가될 때만 동작하게 됩니다.
     */
    @Bean
    public FilterRegistrationBean<GatewaySecretValidatorFilter> gatewaySecretValidatorFilterRegistration(
            GatewaySecretValidatorFilter filter) {
        FilterRegistrationBean<GatewaySecretValidatorFilter> registrationBean = new FilterRegistrationBean<>(filter);
        registrationBean.setEnabled(false); // 서블릿 컨테이너에 자동으로 등록하지 않도록 설정
        return registrationBean;
    }
}
