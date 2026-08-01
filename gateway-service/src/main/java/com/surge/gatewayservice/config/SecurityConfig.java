package com.surge.gatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF (Cross-Site Request Forgery) because we are using stateless JWTs, not browser cookies.
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Define our endpoint rules
                .authorizeHttpRequests(auth -> auth
                        // Open the login/auth endpoints to the public
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Allow all other requests to pass through Spring Security.
                        // Don't worry, they will still be caught by our custom AuthenticationFilter right after this!
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}