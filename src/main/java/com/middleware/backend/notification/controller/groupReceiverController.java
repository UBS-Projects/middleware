package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.GroupReceiversDto;
import com.middleware.backend.notification.dto.GroupReceiversDto2;
import com.middleware.backend.notification.service.NotificationGroupService;
import com.middleware.backend.notification.service.groupReceiverService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups-receivers")
@AllArgsConstructor
public class groupReceiverController {

    private final groupReceiverService service;

    /**
     * Retrieves all groups that are not linked to any receivers.
     *
     * @return 200 with list of unlinked groups
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('group:view')")
    @Operation(
            summary = "List unlinked groups",
            description = "Retrieves all groups that are not currently linked to any receivers."
    )
    public ResponseEntity<?> getUnlinkedGroups() {
        return ResponseEntity.ok(service.getUnlinkedGroups());
    }

    /**
     * Adds a list of receivers to a specific group.
     *
     * @param dto group and receiver details
     * @return 200 with updated group-receiver association details
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority('groupReceivers:create')")
    @Operation(
            summary = "Add receivers to group",
            description = "Adds one or more receivers to a specific group, creating a new group-receiver relationship."
    )
    public ResponseEntity<?> addReceivers(@RequestBody GroupReceiversDto dto) {
        return ResponseEntity.ok(service.addReceivers(dto));
    }

    /**
     * Retrieves all receivers, optionally filtered by search keyword.
     *
     * @param search optional search keyword (matches receiver name or identifier)
     * @return 200 with list of receivers matching the criteria
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('receiver:view')")
    @Operation(
            summary = "List all receivers",
            description = "Retrieves all receivers in the system with optional search filtering by name or identifier."
    )
    public ResponseEntity<?> getAllReceivers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.getReceivers(search));
    }

    /**
     * Retrieves a paginated list of group-receiver relationships.
     *
     * @param page zero-based page index
     * @param size page size
     * @param sortedBy property to sort by (default \"updatedAt\")
     * @param sortDirection sort order: \"asc\" or \"desc\"
     * @return 200 with a paginated list of group-receiver mappings
     */
    @GetMapping("/table")
    @PreAuthorize("hasAuthority('group:view')")
    @Operation(
            summary = "List group-receiver relationships (paginated)",
            description = "Retrieves a paginated list of all group-receiver relationships, supporting sorting and pagination."
    )
    public ResponseEntity<?> getGroupReceivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending()
        );
        return ResponseEntity.ok(service.getGroupReceivers(pageable));
    }

    /**
     * Retrieves receivers for a specific group by name.
     *
     * @param groupName name of the group
     * @return 200 with receiver list of the specified group
     */
    @GetMapping("/specific/{groupName}")
    @PreAuthorize("hasAuthority('group:view')")
    @Operation(
            summary = "Get receivers of specific group",
            description = "Retrieves all receivers associated with a specific group by its name."
    )
    public ResponseEntity<?> getSpecificGroupReceivers(@PathVariable String groupName) {
        return ResponseEntity.ok(service.getGroup(groupName));
    }

    /**
     * Updates receivers for a specific group.
     *
     * @param body updated group and receiver details
     * @return 200 with updated group-receiver mapping information
     */
    @PutMapping("/specific")
    @PreAuthorize("hasAuthority('groupReceivers:edit')")
    @Operation(
            summary = "Update specific group receivers",
            description = "Updates the receiver list of a specific group with the provided group-receiver data."
    )
    public ResponseEntity<?> updateSpecificGroupReceivers(@RequestBody GroupReceiversDto body) {
        return ResponseEntity.ok(service.updateGroup(body));
    }
}