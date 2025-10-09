package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.ReceiverDto;
import com.middleware.backend.notification.model.Receiver;
import com.middleware.backend.notification.service.ReceiverService;
import com.middleware.backend.notification.specification.ReceiverSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
/**
 * REST controller for managing Receivers within the notification system.
 * Provides endpoints to create, update, retrieve, and filter receiver entities.
 *
 * Endpoints:
 * - GET /api/receivers/{id} → Fetch receiver by ID.
 * - GET /api/receivers → Fetch all receivers (with filters, pagination, and sorting).
 * - POST /api/receivers → Create a new receiver.
 * - PUT /api/receivers → Update an existing receiver.
 *
 * All endpoints require appropriate authority permissions defined via Spring Security.
 */
@RestController
@RequestMapping("/api/receivers")
@AllArgsConstructor
public class ReceiverController {

    private final ReceiverService service;

    /**
     * Retrieves a specific receiver by its unique ID.
     *
     * @param id The receiver ID.
     * @return ResponseEntity containing the receiver details.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('receiver:view')")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * Retrieves all receivers with optional filters, pagination, and sorting.
     *
     * Available filters:
     * - name: partial match on receiver's name.
     * - number: partial match on receiver's phone number.
     * - email: partial match on receiver's email address.
     * - createdAfter / createdBefore: date range filtering.
     *
     * Sorting:
     * - Controlled by "sortedBy" (default: updatedAt) and "sortDirection" (asc/desc).
     *
     * Pagination:
     * - Controlled by "page" (default: 0) and "size" (default: 10).
     *
     * @param name Optional filter for receiver name.
     * @param number Optional filter for phone number.
     * @param email Optional filter for email address.
     * @param createdAfter Filter for records created after this date.
     * @param createdBefore Filter for records created before this date.
     * @param page Page index (default 0).
     * @param size Page size (default 10).
     * @param sortedBy Field name to sort by (default: updatedAt).
     * @param sortDirection Sort order ("asc" or "desc").
     * @return Paginated list of receivers matching the criteria.
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('receiver:view')")
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String email,
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

        Specification<Receiver> spec = Specification
                .where(ReceiverSpecification.hasField("name", name, ReceiverSpecification.MatchMode.CONTAINS))
                .and(ReceiverSpecification.hasField("phone", number, ReceiverSpecification.MatchMode.CONTAINS))
                .and(ReceiverSpecification.hasField("email", email, ReceiverSpecification.MatchMode.CONTAINS))
                .and(ReceiverSpecification.dateAfter("createdAt", createdAfter))
                .and(ReceiverSpecification.dateBefore("createdAt", createdBefore));

        return ResponseEntity.ok(service.findAll(spec, pageable));
    }

    /**
     * Creates a new receiver entry in the system.
     *
     * @param dto Receiver data transfer object containing name, phone, and email.
     * @return The created receiver record.
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority('receiver:create')")
    public ResponseEntity<?> create(@RequestBody ReceiverDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    /**
     * Updates an existing receiver's details.
     *
     * @param dto Receiver data transfer object containing updated information.
     * @return The updated receiver record.
     */
    @PutMapping("")
    @PreAuthorize("hasAuthority('receiver:edit')")
    public ResponseEntity<?> update(@RequestBody ReceiverDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }
}
