//package com.middleware.backend.users.config;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//import java.util.List;
//
///**
// * Utility class for handling JSON Web Token (JWT) generation and validation.
// * <p>
// * This class provides methods to create JWT tokens, extract claims,
// * validate tokens, and retrieve custom claims like roles, permissions, and routes.
// * </p>
// *
// * <p>
// * The secret key used for signing tokens is injected from application properties
// * using the {@code jwt.secret} property.
// * </p>
// */
//@Component
//public class JwtUtil {
//
//    /**
//     * Secret key used to sign and verify JWT tokens.
//     * Loaded from the application property {@code jwt.secret}.
//     */
//    @Value("${jwt.secret}")
//    private String secret;
//
//    /**
//     * Generates a JWT token for a user.
//     *
//     * @param email              the email (subject) of the user
//     * @param roles              list of roles assigned to the user
//     * @param permissions        list of permissions assigned to the user
//     * @param routes             list of allowed routes for the user
//     * @param active             whether the user is active
//     * @param customExpirationMs custom expiration time in milliseconds
//     * @return a signed JWT token string
//     */
//    public String generateToken(String email,
//                                List<String> roles,
//                                List<String> permissions,
//                                List<String> routes,
//                                boolean active,
//                                long customExpirationMs) {
//        return Jwts.builder()
//                .setSubject(email)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + customExpirationMs))
//                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
//                .claim("roles", roles)
//                .claim("permissions", permissions)
//                .claim("routes", routes)
//                .claim("active", active)
//                .compact();
//    }
//
//    /**
//     * Extracts the email (subject) from the given JWT token.
//     *
//     * @param token the JWT token
//     * @return the email stored as the subject in the token
//     */
//    public String extractEmail(String token) {
//        return Jwts.parser()
//                .setSigningKey(secret.getBytes())
//                .parseClaimsJws(token)
//                .getBody()
//                .getSubject();
//    }
//
//    /**
//     * Validates a JWT token against the provided user details.
//     * <p>
//     * A token is considered valid if:
//     * <ul>
//     *   <li>The email in the token matches the username in {@link UserDetails}.</li>
//     *   <li>The token is not expired.</li>
//     * </ul>
//     * </p>
//     *
//     * @param token       the JWT token to validate
//     * @param userDetails the user details to check against
//     * @return {@code true} if the token is valid, {@code false} otherwise
//     */
//    public boolean validateToken(String token, UserDetails userDetails) {
//        final String email = extractEmail(token);
//        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
//    }
//
//    /**
//     * Checks if a token has expired.
//     *
//     * @param token the JWT token
//     * @return {@code true} if the token is expired, {@code false} otherwise
//     */
//    private boolean isTokenExpired(String token) {
//        Date expiration = Jwts.parser()
//                .setSigningKey(secret.getBytes())
//                .parseClaimsJws(token)
//                .getBody()
//                .getExpiration();
//        return expiration.before(new Date());
//    }
//
//    /**
//     * Extracts all claims from the given JWT token.
//     *
//     * @param token the JWT token
//     * @return a {@link Claims} object containing all claims in the token
//     */
//    public Claims extractAllClaims(String token) {
//        return Jwts.parser()
//                .setSigningKey(secret.getBytes())
//                .parseClaimsJws(token)
//                .getBody();
//    }
//
//    /**
//     * Extracts the list of routes from the JWT token.
//     *
//     * @param token the JWT token
//     * @return a list of routes contained in the token, or {@code null} if not present
//     */
//    @SuppressWarnings("unchecked")
//    public List<String> extractRoutes(String token) {
//        Claims claims = extractAllClaims(token);
//        return claims.get("routes", List.class);
//    }
//}
