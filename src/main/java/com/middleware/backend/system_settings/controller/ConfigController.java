package com.middleware.backend.system_settings.controller;

import com.middleware.backend.system_settings.dto.ConfigDto;
import com.middleware.backend.system_settings.mapper.ConfigMapper;
import com.middleware.backend.system_settings.model.Config;
import com.middleware.backend.system_settings.model.ConfigType;
import com.middleware.backend.system_settings.service.ConfigService;
import com.middleware.backend.system_settings.specificaion.ConfigSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/configs")
@AllArgsConstructor
public class ConfigController {

    private final ConfigService configService;




    @GetMapping("/{key}")
    @PreAuthorize("hasAuthority('config:view')")
    public ResponseEntity<?> getConfigDetails(
            @PathVariable String key){
        return ResponseEntity.ok(configService.getConfigDetails(key));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('config:view')")
    public ResponseEntity<Page<?>> getAllConfigs(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String value,
            @RequestParam(required = false) String type, // string from request
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size,
                sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortedBy).ascending() : Sort.by(sortedBy).descending());

        // Convert string to enum if valid
        ConfigType configType = null;
        if (type != null) {
            try {
                configType = ConfigType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                // invalid type string, ignore
            }
        }

        Specification<Config> spec = Specification
                .where(ConfigSpecification.hasField("key", key, ConfigSpecification.MatchMode.CONTAINS))
                .and(ConfigSpecification.hasField("value", value, ConfigSpecification.MatchMode.CONTAINS))
                .and(ConfigSpecification.hasType(configType))
                .and(ConfigSpecification.dateAfter("createdAt", createdAfter))
                .and(ConfigSpecification.dateBefore("createdAt", createdBefore));

        return ResponseEntity.ok(configService.findAll(spec, pageable));
    }


    @PostMapping
    @PreAuthorize("hasAuthority('config:create')")
    public ResponseEntity<?> createConfig(@RequestBody ConfigDto dto) {
        Config saved = configService.saveConfig(dto);
        if (saved==null)
            return ResponseEntity.badRequest().body("Key Already Exists");
        return ResponseEntity.ok(ConfigMapper.toDTO(saved));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('config:edit')")
    public ResponseEntity<?> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        ConfigDto updated = configService.updateConfig(key, value);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.badRequest().body("Not Found");
    }
}
