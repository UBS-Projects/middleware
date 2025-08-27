//package com.middleware.backend.kaotocamel.config;
//
//import com.middleware.backend.users.Roles.service.PermissionService;
//import com.middleware.backend.users.Roles.service.RoutesPermissionsService;
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import org.apache.camel.Exchange;
//import org.apache.camel.Processor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.stereotype.Component;
//
//import java.nio.file.AccessDeniedException;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Component
//public class RouteAuthProcessor implements Processor {
//
//    private final String secret;
//
//    // Constructor injection
//    public RouteAuthProcessor(@Value("${jwt.secret}") String secret) {
//        this.secret = secret;
//    }
//
//    @Override
//    public void process(Exchange exchange) throws Exception {
//        System.out.println("******************************************");
//        System.out.println("******************************************");
//        System.out.println("******************************************");
//
//        String routeId = exchange.getFromRouteId();
//        System.out.println(routeId);
//
//        String authHeader = exchange.getIn().getHeader("Authorization", String.class);
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            throw new AccessDeniedException("Missing or invalid Authorization header");
//        }
//
//        String token = authHeader.substring(7); // remove "Bearer "
//
//        // Use injected secret
//        Claims claims = Jwts.parser()
//                .setSigningKey(secret.getBytes())
//                .parseClaimsJws(token)
//                .getBody();
//
//        List<String> allowedRoutes = claims.get("routes", List.class);
//        System.out.println("******************************************");
//        System.out.println(allowedRoutes);
//        if (allowedRoutes == null || !allowedRoutes.contains(routeId)) {
//            throw new AccessDeniedException("User not allowed to access route: " + routeId);
//        }
//    }
//}
