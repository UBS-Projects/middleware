package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.NotificationGroupDto;
import com.middleware.backend.notification.model.NotificationGroup;
import com.middleware.backend.notification.service.NotificationGroupService;
import com.middleware.backend.notification.specification.NotificationGroupSpecification;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller responsible for managing notification groups.
 * <p>
 * Provides endpoints for creating, updating, retrieving, filtering,
 * and changing the status of notification groups.
 */
@RestController
@RequestMapping("/api/groups")
@AllArgsConstructor
public class NotificationGroupController {

    private final NotificationGroupService service;

    /**
     * Retrieves a specific notification group by its ID.
     *
     * @param id the ID of the notification group to retrieve
     * @return {@link ResponseEntity} containing the notification group data
     */
    @Operation(
            summary = "Get Notification Group by ID",
            description = "Retrieves the details of a specific notification group using its unique identifier."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('group:view')")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * Retrieves a paginated list of notification groups with optional filters and sorting.
     *
     * @param name          optional filter by group name (partial match)
     * @param code          optional filter by group code (partial match)
     * @param description   optional filter by description (partial match)
     * @param createdAfter  optional filter for groups created after this date
     * @param createdBefore optional filter for groups created before this date
     * @param page          page number (default 0)
     * @param size          number of records per page (default 10)
     * @param sortedBy      field name to sort by (default "updatedAt")
     * @param sortDirection sort direction, either "asc" or "desc" (default "desc")
     * @return paginated list of filtered notification groups
     */
    @Operation(
            summary = "Get All Notification Groups (Paginated)",
            description = "Retrieves a paginated and sortable list of notification groups with optional filters based on name, code, description, and creation date."
    )
    @GetMapping("")
    @PreAuthorize("hasAuthority('group:view')")
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size,
                sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending());

        Specification<NotificationGroup> spec = Specification
                .where(NotificationGroupSpecification.hasField("name", name, NotificationGroupSpecification.MatchMode.CONTAINS))
                .and(NotificationGroupSpecification.hasField("code", code, NotificationGroupSpecification.MatchMode.CONTAINS))
                .and(NotificationGroupSpecification.hasField("description", description, NotificationGroupSpecification.MatchMode.CONTAINS))
                .and(NotificationGroupSpecification.dateAfter("createdAt", createdAfter))
                .and(NotificationGroupSpecification.dateBefore("createdAt", createdBefore));

        return ResponseEntity.ok(service.findAll(spec, pageable));
    }

    /**
     * Creates a new notification group.
     *
     * @param dto the {@link NotificationGroupDto} containing group details
     * @return {@link ResponseEntity} with the created group information
     */
    @Operation(
            summary = "Create Notification Group",
            description = "Creates a new notification group using the provided details in the request body."
    )
    @PostMapping("")
    @PreAuthorize("hasAuthority('group:create')")
    public ResponseEntity<?> create(@RequestBody NotificationGroupDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    /**
     * Updates an existing notification group.
     *
     * @param dto the {@link NotificationGroupDto} containing updated group details
     * @return {@link ResponseEntity} with the updated group information
     */
    @Operation(
            summary = "Update Notification Group",
            description = "Updates an existing notification group with new information provided in the request body."
    )
    @PutMapping("")
    @PreAuthorize("hasAuthority('group:edit')")
    public ResponseEntity<?> update(@RequestBody NotificationGroupDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }

    /**
     * Retrieves all notification groups without pagination, optionally filtered by a search term.
     *
     * @param search optional search string for filtering results
     * @return list of all matching notification groups
     */
    @Operation(
            summary = "Get All Notification Groups (No Pagination)",
            description = "Retrieves all notification groups without pagination, optionally filtered by a search keyword."
    )
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('group:view')")
    public ResponseEntity<?> getAllGroups(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.getAllGroups(search));
    }

    /**
     * Changes the active status of a specific notification group.
     *
     * @param id the ID of the group to update
     * @return {@link ResponseEntity} with no content on success
     */
    @Operation(
            summary = "Change Notification Group Status",
            description = "Toggles the active/inactive status of a specific notification group using its ID."
    )
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('group:edit')")
    public ResponseEntity<?> changeStatus(@PathVariable Long id) {
        service.changeStatus(id);
        return ResponseEntity.noContent().build();
    }
}
