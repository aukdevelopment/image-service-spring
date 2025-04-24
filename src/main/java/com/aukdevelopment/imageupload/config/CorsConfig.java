package com.aukdevelopment.imageupload.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 🔒 Set allowed origin(s) — use your actual frontend domain in prod
        config.setAllowedOrigins(List.of("http://localhost:5173/"));

        // Allow cookies or Authorization headers (for JWT, session auth, etc.)
        config.setAllowCredentials(true);

        // Headers required for secure upload/auth
        config.setAllowedHeaders(List.of(
                "Content-Type", "Authorization", "X-Requested-With"
        ));

        // Only allow necessary HTTP methods
        config.setAllowedMethods(List.of(
                "GET", "POST", "OPTIONS", "PUT", "DELETE"
        ));

        // Preflight caching (browser will cache CORS preflight for 30 min)
        config.setMaxAge(1800L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
