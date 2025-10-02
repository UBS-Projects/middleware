package com.middleware.backend.users.profile_settings.controller;

import com.middleware.backend.users.profile_settings.dto.ProfileRequest;
import com.middleware.backend.users.profile_settings.dto.ProfileResponse;
import com.middleware.backend.users.profile_settings.service.SettingsService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-profile")
@AllArgsConstructor
public class SettingsController {
    private final SettingsService service;

    @GetMapping()
    public ResponseEntity<ProfileResponse> getInfo(){
        return ResponseEntity.ok(service.getInformation());
    }
    @PatchMapping()
    public ResponseEntity<ProfileResponse> updateProfile(@RequestBody ProfileRequest profile){
        ProfileResponse res = service.updateInfo(profile);
        if(res==null)
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(res);
    }
}
