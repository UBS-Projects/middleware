package com.middleware.backend.users.config;


import com.middleware.backend.users.model.User;
import com.middleware.backend.users.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Loads application users for Spring Security authentication.
 * <p>
 * Users are fetched from the database only if ACTIVE and their authorities are
 * composed of both role names (prefixed with ROLE_) and fine-grained
 * permissions derived from those roles.
 */
@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AppSecurityProps appSecurityProps;
    /**
     * Loads a user by email (case-insensitive) and builds a Spring Security
     * {@link UserDetails} with roles and permissions as authorities.
     *
     * @param email user email used as username
     * @return a populated {@link UserDetails}
     * @throws UsernameNotFoundException if user is not found or not active
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findActiveByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Get authorities from roles + permissions
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName())) // add roles
                .collect(Collectors.toList());

// Add permissions (from roles or directly assigned)
        List<GrantedAuthority> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toList());

// Merge roles + permissions
        authorities.addAll(permissions);

        String dummyPassword = "{noop}KEYCLOAK_USER";

        return org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password(appSecurityProps.authMode().equalsIgnoreCase("application")?user.getPassword():dummyPassword)
                .authorities(authorities)
                .build();
    }

}