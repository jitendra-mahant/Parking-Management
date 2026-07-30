package com.parking.parking_backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test/secure")
    public String secureEndpoint(Authentication authentication) {
        return "Hello " + authentication.getName() + "! Your authorities: " + authentication.getAuthorities();
    }
}