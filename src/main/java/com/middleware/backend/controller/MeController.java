package com.middleware.backend.controller;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MeController {
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        List<String> auths = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return Map.of("sub", jwt.getSubject(), "authorities", auths);
    }
}
