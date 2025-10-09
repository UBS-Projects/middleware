package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.ChannelConfigDto;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.ChannelConfig;
import com.middleware.backend.notification.service.ChannelService;
import com.middleware.backend.notification.specification.ChannelConfigSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/channels")
@AllArgsConstructor
public class ChannelController {

    private final ChannelService service;

    /**
     * Retrieves a channel configuration by its ID.
     *
     * @param id channel configuration ID
     * @return 200 with channel configuration details or 404 if not found
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('channel:view')")
    @Operation(
            summary = "Get channel by ID",
            description = "Retrieves a specific channel configuration by its unique ID."
    )
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * Retrieves a paginated list of channel configurations with optional filters.
     *
     * @param name contains match on channel name
     * @param code contains match on channel code
     * @param config contains match on channel configuration details
     * @param type exact match on channel type (e.g., EMAIL, SMS, PUSH)
     * @param createdAfter filter channels created on/after this date
     * @param createdBefore filter channels created on/before this date
     * @param page zero-based page index
     * @param size page size
     * @param sortedBy property to sort by (default "updatedAt")
     * @param sortDirection sort order: "asc" or "desc"
     * @return 200 with a paginated list of channels matching the criteria
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('channel:view')")
    @Operation(
            summary = "List channels",
            description = "Retrieves a paginated list of channel configurations with optional filters by name, code, type, and creation date range. Supports sorting by any field."
    )
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String config,
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

        ChannelType channelType = null;
        if (type != null) {
            try {
                channelType = ChannelType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore invalid type string
            }
        }

        Specification<ChannelConfig> spec = Specification
                .where(ChannelConfigSpecification.hasField("name", name, ChannelConfigSpecification.MatchMode.CONTAINS))
                .and(ChannelConfigSpecification.hasField("code", code, ChannelConfigSpecification.MatchMode.CONTAINS))
                .and(ChannelConfigSpecification.hasField("config", config, ChannelConfigSpecification.MatchMode.CONTAINS))
                .and(ChannelConfigSpecification.hasType(channelType))
                .and(ChannelConfigSpecification.dateAfter("createdAt", createdAfter))
                .and(ChannelConfigSpecification.dateBefore("createdAt", createdBefore));

        return ResponseEntity.ok(service.findAll(spec, pageable));
    }

    /**
     * Creates a new channel configuration.
     *
     * @param dto channel configuration details
     * @return 200 with created channel configuration details
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority('channel:create')")
    @Operation(
            summary = "Create channel",
            description = "Creates a new channel configuration with the provided details."
    )
    public ResponseEntity<?> create(@RequestBody ChannelConfigDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    /**
     * Updates an existing channel configuration.
     *
     * @param dto updated channel configuration details
     * @return 200 with updated channel configuration details
     */
    @PutMapping("")
    @PreAuthorize("hasAuthority('channel:edit')")
    @Operation(
            summary = "Update channel",
            description = "Updates an existing channel configuration using the provided details."
    )
    public ResponseEntity<?> update(@RequestBody ChannelConfigDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }

    /**
     * Changes the active status of a channel configuration.
     *
     * @param id channel configuration ID
     * @return 204 No Content on successful status change
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('channel:edit')")
    @Operation(
            summary = "Change channel status",
            description = "Toggles the active status of a specific channel configuration."
    )
    public ResponseEntity<?> changeStatus(@PathVariable Long id) {
        service.changeStatus(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all channels, optionally filtered by search keyword.
     *
     * @param search optional search keyword (matches name or code)
     * @return 200 with list of channels matching the search criteria
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('channel:view')")
    @Operation(
            summary = "List all channels (non-paginated)",
            description = "Retrieves all available channels with optional search filtering."
    )
    public ResponseEntity<?> getChannels(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.getChannels(search));
    }
}
