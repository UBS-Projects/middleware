package com.middleware.backend.users.auth;

import com.middleware.backend.users.Roles.model.Permission;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.Roles.model.RoutesPermissions;
import com.middleware.backend.users.config.JwtUtil;
import com.middleware.backend.users.keycloak.dto.KeycloakTokenResponse;
import com.middleware.backend.users.keycloak.dto.KeycloakUser;
import com.middleware.backend.users.keycloak.service.KeycloakService;
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
import org.springframework.http.HttpHeaders;
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

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * REST endpoints for authentication and token management.
 * <p>
 * Provides login/logout for end users and a restricted endpoint to generate
 * service-user tokens with configurable expiration. Issued JWTs embed roles,
 * permissions, allowed route identifiers, and the user's active status.
 */
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final KeycloakService keycloakService;

//    ************************************* Keycloak Login and Logout ************************************************************

    /**
     * Handles the Keycloak authorization code callback and completes the SSO login.
     * <p>
     * Exchanges the authorization code issued by Keycloak for identity tokens, verifies
     * the user exists in the Middleware system, and generates a local JWT token that
     * contains user roles, permissions, allowed route IDs, and active account status.
     * <p>
     * The JWT is stored in the token store and returned by redirecting the user to the
     * front-end dashboard, allowing the system to maintain its own authorization logic
     * while using Keycloak strictly for authentication (SSO).
     *
     * @param code the Keycloak authorization code passed to the redirect URI
     * @return HTTP 302 redirect to front-end with generated token and user info;
     *         401 if the user does not exist in Middleware
     */
    @GetMapping("/keycloak/exchange")
    @Operation(
            summary = "Keycloak login callback",
            description = "Handles Keycloak authorization code exchange, verifies user in Middleware, "
                    + "generates local JWT, and redirects back to the frontend."
    )
    public ResponseEntity<?> kcExchange(@RequestParam String code) {

        KeycloakTokenResponse kcToken = keycloakService.exchangeCode(code);
        KeycloakUser kcUser = keycloakService.getUserInfo(kcToken.getAccess_token());
        String kcIdToken = kcToken.getId_token();


        Optional<User> userOpt = userRepository.findActiveByEmail(kcUser.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User not found in Middleware!");
        }

        User user = userOpt.get();

        String jwt = jwtUtil.generateToken(
                user.getEmail(),
                user.getRoles().stream().map(Role::getRoleName).toList(),
                user.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .map(Permission::getName).distinct().toList(),
                user.getRoles().stream()
                        .flatMap(r -> r.getRoutesPermissions().stream())
                        .map(RoutesPermissions::getRouteId).distinct().toList(),
                user.getStatus().equals(Status.ACTIVE),
                1000 * 60 * 60 * 8 // 8 hours
        );

        tokenService.save(Token.builder()
                .user(user)
                .token(jwt)
                .isValid(true)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .expiresAt(new Timestamp(System.currentTimeMillis() + 28800000))
                .build()
        );

        String redirectUrl = "http://127.0.0.1:5500/html/dashboard.html"
                + "?token=" + jwt
                + "&kcIdToken=" + kcIdToken
                + "&userName=" + user.getUserName();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }


    /**
     * Logs out a user in Keycloak SSO mode.
     * <p>
     * Invalidates the user's local JWT token in the Middleware token store and, when
     * a refresh token is provided, triggers a logout request in Keycloak to terminate
     * the SSO session globally.
     * <p>
     * Ensures the user session is fully terminated both in Keycloak and the Middleware.
     *
     * @param req request payload containing JWT and optional Keycloak refresh token
     * @return 200 OK on successful logout; 400/404 for invalid requests
     */
    @PostMapping("/logout-keycloak")
    @Operation(
            summary = "Keycloak logout",
            description = "Invalidates the user's local JWT and performs Keycloak logout when refresh token is provided."
    )
    public ResponseEntity<?> logout(@RequestBody TokenDto2 req) {
        if (req.getRefreshToken() != null) {
            keycloakService.logout(req.getRefreshToken());
        }

        // Invalidate local JWT
        return tokenService.logout(jwtUtil.extractEmail(req.getToken()));
    }



//    ************************************* Application Login and logout ************************************************************
    /**
     * Authenticates a user using Middleware's internal authentication mechanism.
     * <p>
     * Accepts email and password, validates credentials, and issues a signed JWT
     * containing user roles, permissions, allowed route IDs, and active status. The
     * token is persisted for allow-list validation and defaults to an 8-hour lifespan.
     * <p>
     * Used only when the system is running in "application" authentication mode
     * (Keycloak SSO disabled).
     *
     * @param request credentials payload containing email and password
     * @return 200 OK with {@link AuthResponse2} containing the JWT and user details;
     *         404 if the account belongs to system services
     */
    @PostMapping("/login")
    @Operation(
            summary = "Application login",
            description = "Authenticates user using local credentials and returns a signed JWT with roles and permissions."
    )
    public ResponseEntity<AuthResponse2> login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword())
        );
        long expirationMillis = 1000 * 60 * 60 * 8;
        Optional<User> user = userRepository.findActiveByEmail(request.getEmail().toLowerCase());
        List<Role> roles = user.get().getRoles();

        if (!roles.isEmpty() && roles.get(0).getRoleType() == Role.RoleType.SYSTEM_USER) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse2(null,"Not a User"));
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

        return ResponseEntity.ok(new AuthResponse2(user.get().getUserName(), jwt));
    }
    /**
     * Logs out a user when operating in Middleware's internal authentication mode.
     * <p>
     * Invalidates the user's JWT token in the token store, effectively terminating
     * the application session.
     *
     * @param req request containing the JWT token to invalidate
     * @return 200 OK on success; 400/404 if token is invalid or user not found
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Application logout",
            description = "Invalidates the local JWT token, logging out the user from application mode."
    )
    public ResponseEntity<?> logout(@RequestBody AuthResponse req) {
        return tokenService.logout(jwtUtil.extractEmail(req.getToken().toLowerCase()));
    }
//    *************************************************************************************************
    /**
     * Generates a JWT token for the given user id. Intended for service users.
     * <p>
     * Expiration can be specified by either a number of days or a custom date
     * (formatted yyyy-MM-dd). If both are absent, defaults to 1 day.
     *
     * @param id user identifier
     * @param expirationDays optional expiration in days
     * @param customExpirationDate optional ISO date (yyyy-MM-dd) used as absolute expiration date
     * @return 200 OK with {@link AuthResponse} wrapping the token, or 404 if user is not a service user
     */
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

    /**
     * Authenticates a system user and generates a long-lived JWT token.
     *
     * <p>This endpoint is intended for internal or service accounts (system users).
     * Regular users cannot obtain tokens through this API. The generated JWT token:
     * <ul>
     *   <li>Embeds the user's roles, permissions, allowed route identifiers, and status.</li>
     *   <li>Defaults to a 5-year expiration period.</li>
     *   <li>Is stored in the token database for validation and revocation.</li>
     * </ul>
     *
     * @param request payload containing system user's email and password
     * @return 200 OK with {@link AuthResponse} wrapping the JWT;
     *         404 if the user is not a system user
     */

    @PostMapping("/system-user-token")
    @Operation(
            summary = "Generate a JWT token for a system user",
            description = """
                Authenticates a **system user** using email and password, and generates a JWT token 
                that remains valid for the specified number of days (default: 1).  
                
                The generated token includes:
                - User roles  
                - Permissions  
                - Route access information  

                Only **system users** (non-regular users) are allowed to generate this token.  
                Attempting to generate a token for a regular user will return an error.  
                
                **Parameters:**  
                - `days` (query): The number of days before the token expires (default = 1).  
                - `request` (body): Contains `email` and `password` for authentication.  

                **Response:**  
                - Returns the generated JWT token if authentication succeeds.  
                - Returns an error message if the user is not a system user or authentication fails.
                """
    )
    public ResponseEntity<AuthResponse> GenerateSystemUserToken(@RequestBody AuthRequest request, @RequestParam(required = true, defaultValue = "1") Integer days) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword())
        );
        // Expiration: 5 years
        long expirationMillis = 1000L * 60 * 60 * 24 * days;
        Optional<User> user = userRepository.findActiveByEmail(request.getEmail().toLowerCase());
        List<Role> roles = user.get().getRoles();

        // Only system users allowed
        if (!roles.isEmpty() && roles.get(0).getRoleType() == Role.RoleType.USER) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse("Can't Generate Token for this User"));
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

    @Data
    static class TokenDto2{
        private String token;         // Your system JWT
        private String refreshToken;  // Keycloak refresh token
    }
    /**
     * Login request payload containing user credentials.
     */
    @Data
    static class AuthRequest {
        /** user email used as username (case-insensitive). */
        private String email;
        /** raw user password. */
        private String password;
    }

    /**
     * Registration payload (currently unused in this controller).
     */
    @Data
    static class RegisterRequest {
        /** email to register. */
        private String email;
        /** initial password to set. */
        private String password;
    }

    /**
     * Authentication response wrapping a JWT or a message.
     */
    @Data
    @AllArgsConstructor
    static class AuthResponse {
        /** issued JWT token or a message in error scenarios. */
        private String token;
    }

    @Data
    @AllArgsConstructor
    static class AuthResponse2 {
        private String userName;
        /** issued JWT token or a message in error scenarios. */
        private String token;
    }
}
