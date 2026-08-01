package com.surge.gatewayservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Enumeration;

@RestController
public class ManualProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    @RequestMapping("/api/v1/inventory/**")
    public ResponseEntity<String> proxyToInventory(HttpServletRequest request, @RequestBody(required = false) String body) {
        return forwardRequest(request, body, "http://localhost:8082");
    }

    @RequestMapping("/api/v1/orders/**")
    public ResponseEntity<String> proxyToOrders(HttpServletRequest request, @RequestBody(required = false) String body) {
        return forwardRequest(request, body, "http://localhost:8083");
    }

    private ResponseEntity<String> forwardRequest(HttpServletRequest request, String body, String targetHost) {
        String targetUrl = targetHost + request.getRequestURI();
        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            // We skip content-length so RestTemplate can safely recalculate it
            if (!key.equalsIgnoreCase("content-length")) {
                headers.add(key, request.getHeader(key));
            }
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(targetUrl, HttpMethod.valueOf(request.getMethod()), entity, String.class);
    }
}