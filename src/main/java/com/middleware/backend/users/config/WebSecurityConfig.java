package com.middleware.backend.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

@Configuration
@EnableMethodSecurity

public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**","/swagger-ui.html",
                                "/v3/api-docs/**","/swagger-resources/**","/webjars/**","/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthConverter()))
                );

        return http.build();
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> keycloakJwtAuthConverter() {
        return new Converter<Jwt, AbstractAuthenticationToken>() {
            @Override
            public AbstractAuthenticationToken convert(Jwt jwt) {
                Collection<GrantedAuthority> auths = new ArrayList<>();

                Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                if (realmAccess instanceof Map<?, ?> ra) {
                    Object rolesObj = ra.get("roles");
                    if (rolesObj instanceof Collection<?> roles) {
                        for (Object r : roles) auths.add(new SimpleGrantedAuthority(String.valueOf(r)));
                    }
                }

                Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
                if (resourceAccess instanceof Map<?, ?> ra) {
                    Object backend = ra.get("backend-api"); // عدّل الاسم لو مختلف
                    if (backend instanceof Map<?, ?> m) {
                        Object rolesObj = m.get("roles");
                        if (rolesObj instanceof Collection<?> roles) {
                            for (Object r : roles) auths.add(new SimpleGrantedAuthority(String.valueOf(r)));
                        }
                    }
                }

                List<String> perms = jwt.getClaimAsStringList("permissions");
                if (perms != null) for (String p : perms) auths.add(new SimpleGrantedAuthority(p));

                String name = Optional.ofNullable(jwt.getClaimAsString("preferred_username"))
                        .orElse(jwt.getSubject());
                return new JwtAuthenticationToken(jwt, auths, name);
            }
        };
    }
}


//package com.middleware.backend.users.config;
//
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
///**
// * Spring Security HTTP configuration.
// * <p>
// * Configures stateless JWT-based authentication, allows unauthenticated access
// * to login and Swagger endpoints, and installs a custom JWT filter that
// * enforces route-level permissions for dynamic routes.
// */
//@Configuration
//@EnableMethodSecurity
//public class WebSecurityConfig {
//
//    @Autowired
//    private JwtRequestFilter jwtRequestFilter;
//
//    /**
//     * Configures the security filter chain: disables CSRF for stateless APIs,
//     * permits unauthenticated access to login and API docs, sets 401/403
//     * handlers, and adds {@link JwtRequestFilter} before username/password
//     * authentication.
//     */
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/auth/login","/api/auth/system-user-token").permitAll()
//                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**", "/api-docs/**").permitAll() // Swagger UI
//                        .anyRequest().authenticated()
//                )
//                .sessionManagement(sess -> sess
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                )
//                .exceptionHandling(ex -> ex
//                        .authenticationEntryPoint((request, response, authException) -> {
//                            // No token / invalid token
//                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
//                            response.setContentType("application/json");
//                            response.getWriter().write("{\"error\":\"Unauthorized\"}");
//                        })
//                        .accessDeniedHandler((request, response, accessDeniedException) -> {
//                            // Token is valid, but lacks authority
//                            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
//                            response.setContentType("application/json");
//                            response.getWriter().write("{\"error\":\"Forbidden\"}");
//                        })
//                );
//
//        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
//        return http.build();
//    }
//
//    /**
//     * Exposes the {@link AuthenticationManager} from Spring configuration.
//     */
//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//        return config.getAuthenticationManager();
//    }
//}
