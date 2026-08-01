package com.surge.gatewayservice.controller;

import com.surge.gatewayservice.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(
            @RequestParam String username,
            @RequestParam String password) {

        // TODO: Replace this mock with an actual call to an Identity Service or Database User Table
        // Example: boolean isAuthenticated = authService.verifyCredentials(username, password);

        if ("admin".equals(username) && "admin123".equals(password)) {
            String token = jwtUtil.generateToken("admin-001", "ROLE_ADMIN");
            return ResponseEntity.ok(token);
        } else if ("user".equals(username) && "user123".equals(password)) {
            String token = jwtUtil.generateToken("user-101", "ROLE_USER");
            return ResponseEntity.ok(token);
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }
}