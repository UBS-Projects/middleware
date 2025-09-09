package com.middleware.backend.users.auth;

import com.middleware.backend.users.Roles.model.Permission;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.Roles.model.RoutesPermissions;
import com.middleware.backend.users.config.JwtUtil;
import com.middleware.backend.users.model.Status;
import com.middleware.backend.users.model.User;
import com.middleware.backend.users.repository.UserRepository;
import com.middleware.backend.users.tokens.dto.TokenDto;
import com.middleware.backend.users.tokens.model.Token;
import com.middleware.backend.users.tokens.service.TokenService;
import com.rabbitmq.client.Return;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final TokenService tokenService;


    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates a user using email and password and returns a JWT token. " +
                    "The token includes user roles, permissions, and route access information."
    )
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword())
        );
        long expirationMillis = 1000 * 60 * 60 * 8;
        Optional<User> user = userRepository.findActiveByEmail(request.getEmail().toLowerCase());
        List<Role> roles = user.get().getRoles();

        if (!roles.isEmpty() && roles.get(0).getRoleType() == Role.RoleType.SYSTEM_USER) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse("Not a User"));
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail().toLowerCase());
        String jwt = jwtUtil.generateToken(
                userDetails.getUsername(),
                user.get().getRoles().stream().map(Role::getRoleName).toList(),
                user.get().getRoles().stream().flatMap(r -> r.getPermissions().stream())
                        .map(Permission::getName).distinct().toList(),
                user.get().getRoles().stream().flatMap(r -> r.getRoutesPermissions().stream())
                        .map(RoutesPermissions::getRouteId).distinct().toList(),
                user.get().getStatus().equals(Status.ACTIVE),
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

        return ResponseEntity.ok(new AuthResponse(jwt));
    }
    @PostMapping("/logout")
    @Operation(
            summary = "User logout",
            description = "Invalidates the JWT token of the authenticated user, effectively logging them out."
    )
    public ResponseEntity<?> logout(@RequestBody AuthResponse req) {
        return tokenService.logout(jwtUtil.extractEmail(req.getToken().toLowerCase()));
    }
    @PostMapping("/token")
    @PreAuthorize("hasAuthority('user:generate-token')")
    @Operation(
            summary = "Generate JWT token for a user",
            description = "Generates a JWT token for the specified user ID. " +
                    "Supports custom expiration via 'expirationDays' or 'customExpirationDate'. " +
                    "Requires 'user:generate-token' authority."
    )
    @Transactional
    public ResponseEntity<AuthResponse> generateToken(
            @RequestParam Long id,
            @RequestParam(required = false) Integer expirationDays,
            @RequestParam(required = false) String customExpirationDate) {

        // Fetch user
        User user = userRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id " + id));

        List<Role> roles = user.getRoles();

        // Check user type
        if (!roles.isEmpty() && roles.get(0).getRoleType() == Role.RoleType.USER) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse("Not a Service User"));
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail().toLowerCase());

        // Calculate expiration in millis
        long expirationMillis;
        if (customExpirationDate != null && !customExpirationDate.isEmpty()) {
            LocalDate customDate = LocalDate.parse(customExpirationDate); // yyyy-MM-dd
            ZonedDateTime zonedCustomDateTime = customDate.atStartOfDay(ZoneId.systemDefault());
            expirationMillis = zonedCustomDateTime.toInstant().toEpochMilli() - System.currentTimeMillis();

            if (expirationMillis <= 0) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse("Custom expiration date must be in the future"));
            }
        } else if (expirationDays != null) {
            expirationMillis = expirationDays * 24L * 60 * 60 * 1000;
        } else {
            expirationMillis = 24L * 60 * 60 * 1000; // Default 1 day
        }

        // Roles, permissions, routes
        List<String> roleNames = roles.stream()
                .map(Role::getRoleName)
                .toList();

        List<String> permissions = roles.stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .toList();

        List<String> routes = roles.stream()
                .flatMap(r -> r.getRoutesPermissions().stream())
                .map(RoutesPermissions::getRouteId)
                .distinct()
                .toList();

        // Generate token

        String jwt = jwtUtil.generateToken(userDetails.getUsername(), roleNames, permissions, routes, user.getStatus().equals(Status.ACTIVE), expirationMillis);

        // Save token
        tokenService.save(Token.builder()
                .user(user)
                .token(jwt)
                .isValid(true)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .expiresAt(new Timestamp(System.currentTimeMillis() + expirationMillis))
                .build()
        );

        return ResponseEntity.ok(new AuthResponse(jwt));
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
