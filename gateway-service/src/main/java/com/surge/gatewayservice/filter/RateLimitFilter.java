package com.surge.gatewayservice.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
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
@Order(2) // Runs AFTER your AuthenticationFilter (which should ideally be @Order(1))
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<byte[]> proxyManager;

    public RateLimitFilter(ProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // ==========================================
        // TEMPORARY BYPASS FOR k6 LOAD TESTING
        // Change this to 'false' to re-enable Bucket4j Rate Limiting
        // ==========================================
        boolean isLoadTest = false;
        if (isLoadTest) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for auth endpoints so users can log in
        if (request.getRequestURI().startsWith("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Identify the user (fallback to IP address if unauthenticated)
        String key = request.getHeader("X-User-Id");
        if (key == null) {
            key = request.getRemoteAddr();
        }

        // The Rule: 1 Request allowed per second.
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(1, Refill.intervally(1, Duration.ofSeconds(1))))
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