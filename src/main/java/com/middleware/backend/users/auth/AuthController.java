package com.middleware.backend.users.auth;

import com.middleware.backend.users.Roles.model.Permission;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.config.JwtUtil;
import com.middleware.backend.users.model.User;
import com.middleware.backend.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        Optional<User> user = userRepository.findByEmail(request.getEmail());
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

                1000 * 60 * 60 * 24 // 1 day
        );
        return new AuthResponse(jwt);
    }

    @PostMapping("/token")
    @Transactional  // optional but recommended to keep session open
    public AuthResponse generateToken(@RequestParam Long id,
                                      @RequestParam(required = false) Integer expirationDays,
                                      @RequestParam(required = false) String customExpirationDate) {

        User user = userRepository.findById(id)
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
        System.out.println("--------------------------------------");
        System.out.println(roles);
        System.out.println("--------------------------------------");
        System.out.println(pers);
        System.out.println("--------------------------------------");


        String jwt = jwtUtil.generateToken(userDetails.getUsername(), roles, pers, expirationMillis);
        return new AuthResponse(jwt);
    }

//    @PostMapping("/register")
//    public String register(@RequestBody RegisterRequest request) {
//        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
//            return "User already exists.";
//        }
//
//        User user = new User();
//        user.setEmail(request.getEmail());
//        user.setPassword(new BCryptPasswordEncoder().encode(request.getPassword()));
//        // user.setRole("SYSTEMUSER"); // Optional, if roles are dynamic
//        userRepository.save(user);
//        return "User registered successfully.";
//    }

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
