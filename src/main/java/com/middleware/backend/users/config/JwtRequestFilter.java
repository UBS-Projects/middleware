package com.middleware.backend.users.config;

import com.middleware.backend.kaotocamel.service.DynamicRouteService;
import com.middleware.backend.users.tokens.repository.TokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.apache.camel.util.AntPathMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@AllArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
    private JwtUtil jwtUtil;
    private UserDetailsService userDetailsService;
    private final DynamicRouteService routeService;
    private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        String email = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            email = jwtUtil.extractEmail(jwt);
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);
            System.out.println("**************^^^^^^^^^^^^^^^^^^^^^^^^^^");
            System.out.println(jwt);
            if (jwtUtil.validateToken(jwt, userDetails)) {
                boolean isTokenValid = tokenRepository.existsByUser_EmailAndTokenAndIsValidTrue(email,jwt);
                if (!isTokenValid) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token is invalid or expired\"}");
                    return;
                }

                // 🔑 Map request path → routeId
                String path = request.getRequestURI();
                AntPathMatcher matcher = new AntPathMatcher();
                String normalizedPath = path.replaceFirst("^/camel", "");
                Pageable page = PageRequest.of(0,100000);
                List<String> dbRoutes = routeService.getActiveRoutes().stream().map(
                        r -> r.getPath()
                ).toList();
                boolean isAdmin = userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                if(path.startsWith("/camel/") && !isAdmin) {

                    String matchedRouteId = null;
                    for (String dbRoute : dbRoutes) {

                        String pattern = dbRoute.replaceAll("\\{[^/]+\\}", "*") + "/**";

                        if (matcher.match(pattern, normalizedPath)) {

                             matchedRouteId = routeService.getRouteIdByPath(dbRoute);
                            break;
                        }
                    }

                    List<String> allowedRoutes = jwtUtil.extractRoutes(jwt);

                    // 🔑 Authorization check
                    if (matchedRouteId == null || allowedRoutes == null || !allowedRoutes.contains(matchedRouteId)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Forbidden: not allowed on this route\"}");
                        return;
                    }
                }
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(request, response);
    }

}
