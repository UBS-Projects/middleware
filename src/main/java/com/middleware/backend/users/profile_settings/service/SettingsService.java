package com.middleware.backend.users.profile_settings.service;

import com.middleware.backend.users.config.AppSecurityProps;
import com.middleware.backend.users.keycloak.service.KeycloakAdminService;
import com.middleware.backend.users.model.User;
import com.middleware.backend.users.profile_settings.dto.ProfileRequest;
import com.middleware.backend.users.profile_settings.dto.ProfileResponse;
import com.middleware.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserRepository userRepo;
    private final KeycloakAdminService keycloakAdminService;
    private final AppSecurityProps appSecurityProps;
    private final PasswordEncoder passwordEncoder;

    public ProfileResponse getInformation() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        return userRepo.findByEmail(email).map(
                u -> ProfileResponse.builder()
                        .userName(u.getUserName())
                        .email(u.getEmail())
                        .roles(u.getRoles().stream().map(r -> r.getRoleName()).collect(Collectors.joining(",")))
                        .createdBy(u.getCreatedBy())
                        .createdAt(u.getCreatedAt())
                        .updatedBy(u.getUpdatedBy())
                        .updatedAt(u.getUpdatedAt())
                        .build()
        ).orElse(null);
    }

    public ProfileResponse updateInfo(ProfileRequest profile) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<User> userOpt = userRepo.findByEmail(email);

        if (userOpt.isEmpty()) return null;
        User user = userOpt.get();

        // ✅ Change username
        if (profile.getUserName() != null && !profile.getUserName().isBlank()) {
            user.setUserName(profile.getUserName());
        }

        // ✅ Change password via Keycloak
        if (profile.getPassword() != null && !profile.getPassword().isBlank()) {
            if (profile.getCurrentPassword() == null || profile.getCurrentPassword().isBlank()) {
                throw new RuntimeException("Current password required");
            }
            if (appSecurityProps.authMode().equalsIgnoreCase("application")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            else{
            // We do not check password locally anymore - Check in Keycloak
            boolean valid = keycloakAdminService.verifyUserCredentials(email, profile.getCurrentPassword());

            if (!valid) {
                throw new RuntimeException("Current password incorrect");
            }

            keycloakAdminService.updatePassword(email, profile.getPassword());
        }
        }
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        user.setUpdatedBy(email);
        userRepo.save(user);

        return getInformation();
    }
}
