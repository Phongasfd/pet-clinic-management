package com.petclinic.petservice.config;

import com.petclinic.petservice.filter.UserContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Business APIs require the gateway identity headers; API docs and error pages don't
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/v3/api-docs/**", "/swagger-ui/**", "/error");
    }
}
