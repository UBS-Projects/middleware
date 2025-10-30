//package com.middleware.backend.users.auth;
//
//import com.middleware.backend.users.Roles.model.Permission;
//import com.middleware.backend.users.Roles.model.Role;
//import com.middleware.backend.users.Roles.model.RoutesPermissions;
//import com.middleware.backend.users.config.JwtUtil;
//import com.middleware.backend.users.model.Status;
//import com.middleware.backend.users.model.User;
//import com.middleware.backend.users.repository.UserRepository;
//import com.middleware.backend.users.tokens.dto.TokenDto;
//import com.middleware.backend.users.tokens.model.Token;
//import com.middleware.backend.users.tokens.service.TokenService;
//import com.rabbitmq.client.Return;
//import io.swagger.v3.oas.annotations.Operation;
//import jakarta.transaction.Transactional;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.web.bind.annotation.*;
//
//import java.sql.Timestamp;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
///**
// * REST endpoints for authentication and token management.
// * <p>
// * Provides login/logout for end users and a restricted endpoint to generate
// * service-user tokens with configurable expiration. Issued JWTs embed roles,
// * permissions, allowed route identifiers, and the user's active status.
// */
//@RestController
//@RequestMapping("/api/auth")
//@AllArgsConstructor
//public class AuthController {
//
//    private final AuthenticationManager authenticationManager;
//    private final JwtUtil jwtUtil;
//    private final UserDetailsService userDetailsService;
//    private final UserRepository userRepository;
//    private final TokenService tokenService;
//
//
//    /**
//     * Authenticates a user by email and password and returns a signed JWT.
//     * <p>
//     * The JWT includes user roles, permissions, allowed route IDs, and whether
//     * the account is active. Tokens are persisted for allow-list validation on
//     * subsequent requests and default to an 8-hour expiration.
//     *
//     * @param request credentials payload containing email and password
//     * @return 200 OK with {@link AuthResponse} that wraps the JWT; 404 if user is a system user
//     */
//    @PostMapping("/login")
//    @Operation(
//            summary = "User login",
//            description = "Authenticates a user using email and password and returns a JWT token. " +
//                    "The token includes user roles, permissions, and route access information."
//    )
//    public ResponseEntity<AuthResponse2> login(@RequestBody AuthRequest request) {
//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword())
//        );
//        long expirationMillis = 1000 * 60 * 60 * 8;
//        Optional<User> user = userRepository.findActiveByEmail(request.getEmail().toLowerCase());
//        List<Role> roles = user.get().getRoles();
//
//        if (!roles.isEmpty() && roles.get(0).getRoleType() == Role.RoleType.SYSTEM_USER) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(new AuthResponse2(null,"Not a User"));
//        }
//
//        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail().toLowerCase());
//        String jwt = jwtUtil.generateToken(
//                userDetails.getUsername(),
//                user.get().getRoles().stream().map(Role::getRoleName).toList(),
//                user.get().getRoles().stream().flatMap(r -> r.getPermissions().stream())
//                        .map(Permission::getName).distinct().toList(),
//                user.get().getRoles().stream().flatMap(r -> r.getRoutesPermissions().stream())
//                        .map(RoutesPermissions::getRouteId).distinct().toList(),
//                user.get().getStatus().equals(Status.ACTIVE),
//                expirationMillis
//        );
//
//        tokenService.save(Token.builder()
//                .user(user.get())
//                .token(jwt)
//                .isValid(true)
//                .createdAt(new Timestamp(System.currentTimeMillis()))
//                .expiresAt(new Timestamp(System.currentTimeMillis() + expirationMillis))
//                .build()
//        );
//
//        return ResponseEntity.ok(new AuthResponse2(user.get().getUserName(), jwt));
//    }
//    /**
//     * Logs out a user by invalidating the provided JWT in the token store.
//     *
//     * @param req response wrapper containing the token to invalidate
//     * @return 200 OK if invalidated; 400/404 for invalid requests
//     */
//    @PostMapping("/logout")
//    @Operation(
//            summary = "User logout",
//            description = "Invalidates the JWT token of the authenticated user, effectively logging them out."
//    )
//    public ResponseEntity<?> logout(@RequestBody AuthResponse req) {
//        return tokenService.logout(jwtUtil.extractEmail(req.getToken().toLowerCase()));
//    }
//    /**
//     * Generates a JWT token for the given user id. Intended for service users.
//     * <p>
//     * Expiration can be specified by either a number of days or a custom date
//     * (formatted yyyy-MM-dd). If both are absent, defaults to 1 day.
//     *
//     * @param id user identifier
//     * @param expirationDays optional expiration in days
//     * @param customExpirationDate optional ISO date (yyyy-MM-dd) used as absolute expiration date
//     * @return 200 OK with {@link AuthResponse} wrapping the token, or 404 if user is not a service user
//     */
//    @PostMapping("/token")
//    @PreAuthorize("hasAuthority('user:generate-token')")
//    @Operation(
//            summary = "Generate JWT token for a user",
//            description = "Generates a JWT token for the specified user ID. " +
//                    "Supports custom expiration via 'expirationDays' or 'customExpirationDate'. " +
//                    "Requires 'user:generate-token' authority."
//    )
//    @Transactional
//    public ResponseEntity<AuthResponse> generateToken(
//            @RequestParam Long id,
//            @RequestParam(required = false) Integer expirationDays,
//            @RequestParam(required = false) String customExpirationDate) {
//
//        // Fetch user
//        User user = userRepository.findActiveById(id)
//                .orElseThrow(() -> new RuntimeException("User not found with id " + id));
//
//        List<Role> roles = user.getRoles();
//
//        // Check user type
//        if (!roles.isEmpty() && roles.get(0).getRoleType() == Role.RoleType.USER) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(new AuthResponse("Not a Service User"));
//        }
//
//        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail().toLowerCase());
//
//        // Calculate expiration in millis
//        long expirationMillis;
//        if (customExpirationDate != null && !customExpirationDate.isEmpty()) {
//            LocalDate customDate = LocalDate.parse(customExpirationDate); // yyyy-MM-dd
//            ZonedDateTime zonedCustomDateTime = customDate.atStartOfDay(ZoneId.systemDefault());
//            expirationMillis = zonedCustomDateTime.toInstant().toEpochMilli() - System.currentTimeMillis();
//
//            if (expirationMillis <= 0) {
//                return ResponseEntity.badRequest()
//                        .body(new AuthResponse("Custom expiration date must be in the future"));
//            }
//        } else if (expirationDays != null) {
//            expirationMillis = expirationDays * 24L * 60 * 60 * 1000;
//        } else {
//            expirationMillis = 24L * 60 * 60 * 1000; // Default 1 day
//        }
//
//        // Roles, permissions, routes
//        List<String> roleNames = roles.stream()
//                .map(Role::getRoleName)
//                .toList();
//
//        List<String> permissions = roles.stream()
//                .flatMap(r -> r.getPermissions().stream())
//                .map(Permission::getName)
//                .distinct()
//                .toList();
//
//        List<String> routes = roles.stream()
//                .flatMap(r -> r.getRoutesPermissions().stream())
//                .map(RoutesPermissions::getRouteId)
//                .distinct()
//                .toList();
//
//        // Generate token
//
//        String jwt = jwtUtil.generateToken(userDetails.getUsername(), roleNames, permissions, routes, user.getStatus().equals(Status.ACTIVE), expirationMillis);
//
//        // Save token
//        tokenService.save(Token.builder()
//                .user(user)
//                .token(jwt)
//                .isValid(true)
//                .createdAt(new Timestamp(System.currentTimeMillis()))
//                .expiresAt(new Timestamp(System.currentTimeMillis() + expirationMillis))
//                .build()
//        );
//
//        return ResponseEntity.ok(new AuthResponse(jwt));
//    }
//
//    /**
//     * Authenticates a system user and generates a long-lived JWT token.
//     *
//     * <p>This endpoint is intended for internal or service accounts (system users).
//     * Regular users cannot obtain tokens through this API. The generated JWT token:
//     * <ul>
//     *   <li>Embeds the user's roles, permissions, allowed route identifiers, and status.</li>
//     *   <li>Defaults to a 5-year expiration period.</li>
//     *   <li>Is stored in the token database for validation and revocation.</li>
//     * </ul>
//     *
//     * @param request payload containing system user's email and password
//     * @return 200 OK with {@link AuthResponse} wrapping the JWT;
//     *         404 if the user is not a system user
//     */
//
//    @PostMapping("/system-user-token")
//    @Operation(
//            summary = "Generate a 5-year JWT token for a system user",
//            description = "Authenticates a system user (via email & password) and generates a JWT "
//                    + "that is valid for 5 years. "
//                    + "The token includes roles, permissions, and route access information. "
//                    + "This endpoint is restricted to system users only."
//    )
//    public ResponseEntity<AuthResponse> GenerateSystemUserToken(@RequestBody AuthRequest request) {
//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword())
//        );
//        // Expiration: 5 years
//        long expirationMillis = 1000L * 60 * 60 * 24 * 365 * 5;
//        Optional<User> user = userRepository.findActiveByEmail(request.getEmail().toLowerCase());
//        List<Role> roles = user.get().getRoles();
//
//        // Only system users allowed
//        if (!roles.isEmpty() && roles.get(0).getRoleType() == Role.RoleType.USER) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(new AuthResponse("Can't Generate Token for this User"));
//        }
//
//        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail().toLowerCase());
//        String jwt = jwtUtil.generateToken(
//                userDetails.getUsername(),
//                user.get().getRoles().stream().map(Role::getRoleName).toList(),
//                user.get().getRoles().stream().flatMap(r -> r.getPermissions().stream())
//                        .map(Permission::getName).distinct().toList(),
//                user.get().getRoles().stream().flatMap(r -> r.getRoutesPermissions().stream())
//                        .map(RoutesPermissions::getRouteId).distinct().toList(),
//                user.get().getStatus().equals(Status.ACTIVE),
//                expirationMillis
//        );
//        tokenService.save(Token.builder()
//                .user(user.get())
//                .token(jwt)
//                .isValid(true)
//                .createdAt(new Timestamp(System.currentTimeMillis()))
//                .expiresAt(new Timestamp(System.currentTimeMillis() + expirationMillis))
//                .build()
//        );
//
//        return ResponseEntity.ok(new AuthResponse(jwt));
//    }
//
//    /**
//     * Login request payload containing user credentials.
//     */
//    @Data
//    static class AuthRequest {
//        /** user email used as username (case-insensitive). */
//        private String email;
//        /** raw user password. */
//        private String password;
//    }
//
//    /**
//     * Registration payload (currently unused in this controller).
//     */
//    @Data
//    static class RegisterRequest {
//        /** email to register. */
//        private String email;
//        /** initial password to set. */
//        private String password;
//    }
//
//    /**
//     * Authentication response wrapping a JWT or a message.
//     */
//    @Data
//    @AllArgsConstructor
//    static class AuthResponse {
//        /** issued JWT token or a message in error scenarios. */
//        private String token;
//    }
//
//    @Data
//    @AllArgsConstructor
//    static class AuthResponse2 {
//        private String userName;
//        /** issued JWT token or a message in error scenarios. */
//        private String token;
//    }
//}
