package com.insuranceai.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    // Externalized so production can point this at the real deployed frontend origin
    // (app.cors.allowed-origins, set via CORS_ALLOWED_ORIGINS in docker-compose.prod.yml)
    // without touching code -- the dev origins stay the default so local `mvn spring-boot:run`
    // behavior is unchanged.
    public WebConfig(@Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
                      String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
