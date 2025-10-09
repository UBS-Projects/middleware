package com.middleware.backend.users.profile_settings.controller;

import com.middleware.backend.users.profile_settings.dto.ProfileRequest;
import com.middleware.backend.users.profile_settings.dto.ProfileResponse;
import com.middleware.backend.users.profile_settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-profile")
@AllArgsConstructor
public class SettingsController {

    private final SettingsService service;

    // ===================== GET USER PROFILE =====================
    @GetMapping()
    @Operation(
            summary = "Get user profile information",
            description = "Retrieves the current user's profile information including name, email, and other profile settings."
    )
    public ResponseEntity<ProfileResponse> getInfo() {
        return ResponseEntity.ok(service.getInformation());
    }

    // ===================== UPDATE USER PROFILE =====================
    @PatchMapping()
    @Operation(
            summary = "Update user profile",
            description = "Updates the current user's profile information. Returns the updated profile or a bad request if update fails."
    )
    public ResponseEntity<ProfileResponse> updateProfile(@RequestBody ProfileRequest profile) {
        ProfileResponse res = service.updateInfo(profile);
        if (res == null)
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(res);
    }
}
