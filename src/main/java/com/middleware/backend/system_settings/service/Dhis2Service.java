package com.middleware.backend.system_settings.service;

import com.middleware.backend.system_settings.dto.Dhis2Dto;
import com.middleware.backend.system_settings.mapper.Dhis2Mapper;
import com.middleware.backend.system_settings.model.Dhis2;
import com.middleware.backend.system_settings.repository.Dhis2Repository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving and persisting DHIS2 system settings.
 */
@Service
@AllArgsConstructor
public class Dhis2Service {

    private final Dhis2Repository repo;

    public Dhis2 getActiveSettings() {
        return repo.findTopByOrderByIdDesc().orElse(null);
    }

    /**
     * Retrieves the current settings as DTO wrapped in HTTP response.
     */
    public ResponseEntity<?> getSettings() {
        Dhis2 settings = getActiveSettings();
        return ResponseEntity.ok(Dhis2Mapper.mapToDto(settings));
    }

    /**
     * Creates or updates DHIS2 settings. Empty strings keep existing non-null values.
     */
    public ResponseEntity<?> save(Dhis2Dto dhis2Dto) {
        Dhis2 existing = repo.findTopByOrderByIdDesc().orElse(new Dhis2());

        Dhis2 entity = Dhis2.builder()
                .id(existing.getId())
                .baseUrl(dhis2Dto.getBaseUrl().isEmpty() ? existing.getBaseUrl() : dhis2Dto.getBaseUrl())
                .userName(dhis2Dto.getUserName().isEmpty() ? existing.getUserName() : dhis2Dto.getUserName())
                .password(dhis2Dto.getPassword().isEmpty() ? existing.getPassword() : dhis2Dto.getPassword())
                .timeout(dhis2Dto.getTimeout() == null ? existing.getTimeout() : dhis2Dto.getTimeout())
                .connectTimeout(dhis2Dto.getConnectTimeout() == null ? existing.getConnectTimeout() : dhis2Dto.getConnectTimeout())
                .build();

        Dhis2 saved = repo.save(entity);
        return ResponseEntity.ok(Dhis2Mapper.mapToDto(saved));
    }
}
