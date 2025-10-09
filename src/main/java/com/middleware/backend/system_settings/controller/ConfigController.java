package com.middleware.backend.system_settings.controller;

import com.middleware.backend.system_settings.dto.ConfigDto;
import com.middleware.backend.system_settings.mapper.ConfigMapper;
import com.middleware.backend.system_settings.model.Config;
import com.middleware.backend.system_settings.model.ConfigType;
import com.middleware.backend.system_settings.service.ConfigService;
import com.middleware.backend.system_settings.specificaion.ConfigSpecification;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.Map;

@RestController
@RequestMapping("/api/configs")
@AllArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    // ===================== GET SINGLE CONFIG =====================

    /**
     * Get details of a specific configuration by key.
     *
     * @param key the configuration key to look up
     * @return ResponseEntity containing the configuration details or 404 if not found
     */
    @GetMapping("/{key}")
    @PreAuthorize("hasAuthority('config:view')")
    @Operation(
            summary = "Get configuration by key",
            description = "Retrieve details of a single configuration by its key. Requires 'config:view' authority."
    )
    public ResponseEntity<?> getConfigDetails(@PathVariable String key) {
        return ResponseEntity.ok(configService.getConfigDetails(key));
    }

    // ===================== GET ALL CONFIGS =====================

    /**
     * Retrieve a paginated list of configurations with optional filters.
     *
     * @param key            optional key filter (contains)
     * @param value          optional value filter (contains)
     * @param type           optional config type filter (exact)
     * @param createdAfter   optional lower bound for creation date (inclusive)
     * @param createdBefore  optional upper bound for creation date (inclusive)
     * @param page           zero-based page index
     * @param size           page size
     * @param sortedBy       field to sort by (default "updatedAt")
     * @param sortDirection  sort direction ("asc" or "desc", default "desc")
     * @return paginated list of configs matching the filters
     */
    @GetMapping
    @PreAuthorize("hasAuthority('config:view')")
    @Operation(
            summary = "List configurations with filters",
            description = "Retrieves a paginated list of configurations with optional filters such as key, value, type, " +
                    "and creation date range. Supports paging and sorting. Requires 'config:view' authority."
    )
    public ResponseEntity<Page<?>> getAllConfigs(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String value,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size,
                sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortedBy).ascending() : Sort.by(sortedBy).descending());

        ConfigType configType = null;
        if (type != null) {
            try {
                configType = ConfigType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore invalid type
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

    // ===================== CREATE CONFIG =====================

    /**
     * Create a new configuration.
     *
     * @param dto configuration data transfer object
     * @return ResponseEntity with the created config, or bad request if key already exists
     */
    @PostMapping
    @PreAuthorize("hasAuthority('config:create')")
    @Operation(
            summary = "Create a new configuration",
            description = "Creates a new configuration with a unique key. Requires 'config:create' authority. " +
                    "Returns bad request if the key already exists."
    )
    public ResponseEntity<?> createConfig(@RequestBody ConfigDto dto) {
        Config saved = configService.saveConfig(dto);
        if (saved == null)
            return ResponseEntity.badRequest().body("Key Already Exists");
        return ResponseEntity.ok(ConfigMapper.toDTO(saved));
    }

    // ===================== UPDATE CONFIG =====================

    /**
     * Update the value of an existing configuration.
     *
     * @param key  the key of the configuration to update
     * @param body a map containing the new value {"value": "..."}
     * @return ResponseEntity with the updated config, or bad request if not found
     */
    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('config:edit')")
    @Operation(
            summary = "Update a configuration",
            description = "Updates the value of an existing configuration by key. Requires 'config:edit' authority. " +
                    "Returns bad request if the key is not found."
    )
    public ResponseEntity<?> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        ConfigDto updated = configService.updateConfig(key, value);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.badRequest().body("Not Found");
    }
}
