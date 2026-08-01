package com.surge.gatewayservice.filter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(2) // Runs AFTER your AuthenticationFilter
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<byte[]> proxyManager;

    public RateLimitFilter(ProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // Fallback to IP address if unauthenticated
        String key = request.getRemoteAddr();

        // Extract the verified Clerk User ID directly from the Spring Security Context
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            key = jwt.getSubject(); // This is the unique "user_xyz123" ID from Clerk
        }

        // Modern Bucket4j 8.x+ API: Using lambda builder instead of deprecated static methods
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(1).refillIntervally(1, Duration.ofSeconds(1)))
                .build();

        // Fetch or create this specific user's bucket in Redis
        Bucket bucket = proxyManager.builder().build(key.getBytes(), () -> configuration);

        // Try to consume 1 token
        if (bucket.tryConsume(1)) {
            // Success: Pass the request to the next filter/downstream service
            filterChain.doFilter(request, response);
        } else {
            // Blocked: User is spamming the API
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too Many Requests - Server is experiencing high load.");
            System.out.println("Blocked spam request from: " + key);
        }
    }
}