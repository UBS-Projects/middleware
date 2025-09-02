package com.middleware.backend.users.auth;

import com.middleware.backend.users.Roles.model.Permission;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.Roles.model.RoutesPermissions;
import com.middleware.backend.users.config.JwtUtil;
import com.middleware.backend.users.model.User;
import com.middleware.backend.users.repository.UserRepository;
import com.middleware.backend.users.tokens.dto.TokenDto;
import com.middleware.backend.users.tokens.model.Token;
import com.middleware.backend.users.tokens.service.TokenService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final TokenService tokenService;


    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        long expirationMillis = 1000 * 60 * 60 * 8;
        Optional<User> user = userRepository.findActiveByEmail(request.getEmail());
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String jwt = jwtUtil.generateToken(
                userDetails.getUsername(),

                // Roles → List<String>
                user.get().getRoles().stream()
                        .map(Role::getRoleName)
                        .toList(),

                // Permissions → List<String>
                user.get().getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream()) // flatten List<List<Permission>>
                        .map(Permission::getName)                  // use the `name` field
                        .distinct()                                // optional: remove duplicates
                        .toList(),
                user.get().getRoles().stream()
                        .flatMap(r -> r.getRoutesPermissions().stream()) // flatten List<List<Permission>>
                        .map(RoutesPermissions::getRouteId)                  // use the `name` field
                        .distinct()                                // optional: remove duplicates
                        .toList(),

                expirationMillis
        );
        tokenService.save(Token.builder()
                .user(user.get())
                .token(jwt)
                .isValid(true)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .expiresAt(new Timestamp(System.currentTimeMillis() + expirationMillis))
                .build()
        );
        return new AuthResponse(jwt);
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody AuthResponse req) {
        return tokenService.logout(jwtUtil.extractEmail(req.getToken()));
    }
    @PostMapping("/token")
    @PreAuthorize("hasAuthority('user:generate-token')")
    @Transactional  // optional but recommended to keep session open
    public AuthResponse generateToken(@RequestParam Long id,
                                      @RequestParam(required = false) Integer expirationDays,
                                      @RequestParam(required = false) String customExpirationDate) {

        User user = userRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id " + id));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

    long expirationMillis;
        if (customExpirationDate != null && !customExpirationDate.isEmpty()) {
            // Parse customExpirationDate and calculate millis from now
            LocalDate customDate = LocalDate.parse(customExpirationDate); // yyyy-MM-dd format expected
            LocalDateTime customDateTime = customDate.atStartOfDay();
            ZonedDateTime zonedCustomDateTime = customDateTime.atZone(ZoneId.systemDefault());
            long customExpirationEpochMillis = zonedCustomDateTime.toInstant().toEpochMilli();

            long nowMillis = System.currentTimeMillis();
            expirationMillis = customExpirationEpochMillis - nowMillis;

            if (expirationMillis <= 0) {
                throw new IllegalArgumentException("Custom expiration date must be in the future");
            }
        } else if (expirationDays != null) {
            expirationMillis = expirationDays * 24L * 60 * 60 * 1000;
        } else {
            // Default expiration 1 day
            expirationMillis = 24L * 60 * 60 * 1000;
        }

        List<String> roles = user.getRoles().stream().map(
                r -> r.getRoleName()
        ).toList();

        List<String> pers = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream()
                        .map(p -> p.getName()))
                .toList();

        List<String> routes = user.getRoles().stream()
                .flatMap(r -> r.getRoutesPermissions().stream()
                        .map(p -> p.getRouteId()))
                .toList();



        String jwt = jwtUtil.generateToken(userDetails.getUsername(), roles, pers,routes, expirationMillis);
        tokenService.save(Token.builder()
                .user(user)
                .token(jwt)
                .isValid(true)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .expiresAt(new Timestamp(System.currentTimeMillis() + expirationMillis))
                .build()
        );
        return new AuthResponse(jwt);
    }

    @Data
    static class AuthRequest {
        private String email;
        private String password;
    }

    @Data
    static class RegisterRequest {
        private String email;
        private String password;
    }

    @Data
    @AllArgsConstructor
    static class AuthResponse {
        private String token;
    }
}
