package com.ecommerce.orderservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class FeignConfig {

    @Value("${internal.service-token}")
    private String internalServiceToken;

    @Bean
    RequestInterceptor bearerTokenInterceptor() {
        return template -> {
            template.header("X-Internal-Service-Token", internalServiceToken);
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                template.header("Authorization", authorization);
            }
        };
    }
}
