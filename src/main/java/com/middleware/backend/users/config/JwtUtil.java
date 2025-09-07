package com.middleware.backend.users.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;


@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;


    public String generateToken(String email, List<String> roles, List<String> permissions,List<String> routes, boolean active, long customExpirationMs) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + customExpirationMs))
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .claim("roles", roles)
                .claim("permissions",permissions)
                .claim("routes",routes)
                .claim("active",active)
                .compact();
    }
    public String extractEmail(String token) {
        return Jwts.parser()
                .setSigningKey(secret.getBytes())
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    public boolean validateToken(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser()
                .setSigningKey(secret.getBytes())
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }


    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret.getBytes())
                .parseClaimsJws(token)
                .getBody();
    }
    public List<String> extractRoutes(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("routes", List.class);

    }
}

