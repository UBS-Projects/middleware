package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.GroupReceiversDto;
import com.middleware.backend.notification.dto.NotificationGroupDto;
import com.middleware.backend.notification.model.NotificationGroup;
import com.middleware.backend.notification.model.NotificationTemplate;
import com.middleware.backend.notification.service.NotificationGroupService;
import com.middleware.backend.notification.specification.NotificationGroupSpecification;
import com.middleware.backend.notification.specification.NotificationTemplateSpecification;
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

@RestController
@RequestMapping("/api/groups")
@AllArgsConstructor
public class NotificationGroupController {
    private final NotificationGroupService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('group:view')")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

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
        Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("asc")
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

    @PostMapping("")
    @PreAuthorize("hasAuthority('group:create')")
    public ResponseEntity<?> create(@RequestBody NotificationGroupDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("")
    @PreAuthorize("hasAuthority('group:edit')")
    public ResponseEntity<?> update(@RequestBody NotificationGroupDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('group:view')")
    public ResponseEntity<?> getAllGroups(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(service.getAllGroups(search));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('group:delete')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

