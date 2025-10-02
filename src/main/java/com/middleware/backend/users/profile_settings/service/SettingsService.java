package com.middleware.backend.users.profile_settings.service;

import com.middleware.backend.users.model.User;
import com.middleware.backend.users.profile_settings.dto.ProfileRequest;
import com.middleware.backend.users.profile_settings.dto.ProfileResponse;
import com.middleware.backend.users.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SettingsService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public ProfileResponse getInformation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        return userRepo.findByEmail(emailUser).map(
                u->
                ProfileResponse.builder()
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        Optional<User> user = userRepo.findByEmail(emailUser);
        if(user.isEmpty())
            return null;
        if(!profile.getUserName().equals(""))
            user.get().setUserName(profile.getUserName());
        if(!profile.getPassword().equals(""))
            user.get().setPassword(passwordEncoder.encode(profile.getPassword()));

        user.get().setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        user.get().setUpdatedBy(emailUser);

        userRepo.save(user.get());

        return userRepo.findByEmail(emailUser).map(
                u->
                        ProfileResponse.builder()
                                .userName(u.getUserName())
                                .email(u.getEmail())
                                .roles(u.getRoles().stream().map(r -> r.getRoleName()).collect(Collectors.joining(",")))
                                .createdBy(u.getCreatedBy())
                                .createdAt(u.getCreatedAt())
                                .updatedBy(u.getUpdatedBy())
                                .updatedAt(u.getUpdatedAt())
                                .build()
        ).get();
    }
}
