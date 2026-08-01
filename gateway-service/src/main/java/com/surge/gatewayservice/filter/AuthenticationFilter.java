package com.surge.gatewayservice.filter;

import com.surge.gatewayservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public AuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }


        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized: Missing or invalid bearer token");
            return;
        }

        String token = authHeader.substring(7);

        try {
            jwtUtil.validateToken(token);
            //validate jwt cryptographically here using secret
            String extractedUserId = jwtUtil.extractUserId(token);
            String extractedRole = jwtUtil.extractRole(token);

            HeaderMapRequestWrapper wrapper = new HeaderMapRequestWrapper(request);
            wrapper.addHeader("X-User-Id", extractedUserId);
            wrapper.addHeader("X-User-Role", extractedRole);

            filterChain.doFilter(wrapper, response);

        }
        catch(Exception e) {
            e.printStackTrace();
            response.getStatus();
            response.getWriter().write("Unauthorized: Invalid token");
        }

    }

}
