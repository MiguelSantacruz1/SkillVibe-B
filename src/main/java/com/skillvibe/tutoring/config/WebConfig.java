package com.skillvibe.tutoring.config;

import com.skillvibe.tutoring.security.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply rate limiting to authentication and registration endpoints
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/auth/login", "/api/auth/register", "/api/auth/register/tutor", "/api/auth/forgot-password");
    }
}
